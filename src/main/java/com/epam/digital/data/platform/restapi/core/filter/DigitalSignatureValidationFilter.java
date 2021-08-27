package com.epam.digital.data.platform.restapi.core.filter;

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_ACCESS_TOKEN;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static org.springframework.util.StringUtils.isEmpty;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.service.DigitalSignatureService;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(FiltersOrder.digitalSignatureValidationFilter)
public class DigitalSignatureValidationFilter implements Filter {

  private static final Set<String> applicableHttpMethods = Set.of("PUT", "POST", "DELETE", "PATCH");

  private final DigitalSignatureService digitalSignatureService;
  private final boolean isEnabled;

  public DigitalSignatureValidationFilter(DigitalSignatureService digitalSignatureService,
      @Value("${data-platform.signature.validation.enabled}") boolean isEnabled) {
    this.digitalSignatureService = digitalSignatureService;
    this.isEnabled = isEnabled;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    String method = request.getMethod().toUpperCase();

    SecurityContext securityContext = new SecurityContext();
    securityContext.setAccessToken(request.getHeader(X_ACCESS_TOKEN.getHeaderName()));

    if (applicableHttpMethods.contains(method)) {

      try {
        fillContextSignatures(securityContext, request);

        String data;
        if (method.equals("DELETE")) {
          data = getDataForDelete(request);
        } else {
          request = new MultiReadHttpServletRequest(request);
          data = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        }

        digitalSignatureService.checkSignature(data, securityContext);
        saveSignatures(securityContext);

      } catch (IllegalArgumentException e) {
        if (isEnabled) {
          throw e;
        }
      }
    }
    request.setAttribute(SecurityContext.class.getSimpleName(), securityContext);
    filterChain.doFilter(request, servletResponse);
  }

  private SecurityContext saveSignatures(SecurityContext sc) {
    String signature = digitalSignatureService.saveSignature(sc.getDigitalSignature());
    sc.setDigitalSignatureChecksum(DigestUtils.sha256Hex(signature));

    if(!isEmpty(sc.getDigitalSignatureDerived())) {
      String signatureDerived = digitalSignatureService.saveSignature(sc.getDigitalSignatureDerived());
      sc.setDigitalSignatureDerivedChecksum(DigestUtils.sha256Hex(signatureDerived));
    }

    return sc;
  }

  private String getDataForDelete(HttpServletRequest request) {
    String fullPath = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    String url = RegExUtils.removePattern(fullPath, "/+$");
    UUID result = UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
    return result.toString();
  }

  private SecurityContext fillContextSignatures(SecurityContext securityContext, HttpServletRequest request) {
    String xDigitalSignature = request.getHeader(X_DIGITAL_SIGNATURE.getHeaderName());
    String xDigitalSignatureDerived = request
        .getHeader(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName());

    securityContext.setDigitalSignature(xDigitalSignature);
    securityContext.setDigitalSignatureDerived(xDigitalSignatureDerived);

    if (isEmpty(xDigitalSignature)) {
      throw new IllegalArgumentException("Missing required Header X-Digital-Signature");
    }

    return securityContext;
  }
}
