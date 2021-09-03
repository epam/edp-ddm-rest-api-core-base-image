package com.epam.digital.data.platform.restapi.core.advice;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import java.util.ArrayList;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FileRequestBodyAspect {

  private final FileService fileService;

  public FileRequestBodyAspect(FileService fileService) {
    this.fileService = fileService;
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void restController() {
  }

  @Pointcut("restController() && @annotation(org.springframework.web.bind.annotation.PostMapping)")
  public void postPointcut() {
  }

  @Pointcut("restController() && @annotation(org.springframework.web.bind.annotation.PutMapping)")
  public void putPointcut() {
  }

  @Pointcut("restController() && @annotation(org.springframework.web.bind.annotation.PatchMapping)")
  public void patchPointcut() {
  }

  @Pointcut("postPointcut() || putPointcut() || patchPointcut()")
  public void modifyingPointcut() {
  }

  @Before("modifyingPointcut() && args(.., dto, context, securityContext)")
  void process(JoinPoint joinPoint, Object dto, RequestContext context,
      SecurityContext securityContext) {

    var notFound = new ArrayList<String>();

    fileService.getFileProperties(dto).forEach(
        f -> {
          var success = fileService.store(context.getBusinessProcessInstanceId(), f.getValue());
          if (!success) {
            notFound.add(f.getName());
          }
        }
    );

    if (!notFound.isEmpty()) {
      throw new FileNotExistsException("Files not found in ceph bucket", notFound);
    }
  }
}
