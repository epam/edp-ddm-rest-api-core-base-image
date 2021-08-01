package com.epam.digital.data.platform.restapi.core.advice;

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID;
import static java.util.Collections.singletonList;

import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import java.util.ArrayList;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class FileResponseBodyAdvice implements ResponseBodyAdvice {

  private final FileService fileService;

  public FileResponseBodyAdvice(
      FileService fileService) {
    this.fileService = fileService;
  }

  @Override
  public boolean supports(MethodParameter returnType, Class converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType,
      MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null) {
      return null;
    }

    var notFound = new ArrayList<String>();

    fileService.getFileProperties(body).forEach(
        f -> {
          var success = fileService.retrieve(getInstanceId(request), f.getValue());
          if (!success) {
            notFound.add(f.getName());
          }
        }
    );

    if (!notFound.isEmpty()) {
      throw new FileNotExistsException("Files not found in ceph bucket", notFound);
    }

    return body;
  }

  private String getInstanceId(ServerHttpRequest request) {
    var vals = request.getHeaders().get(X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName());
    if (vals == null || vals.size() != 1) {
      // should never happen
      throw new MandatoryHeaderMissingException(
          singletonList(X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName()));
    }
    return vals.get(0);
  }
}
