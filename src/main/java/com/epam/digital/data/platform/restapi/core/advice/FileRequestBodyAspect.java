/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.advice;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.service.FilePropertiesService;
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
  private final FilePropertiesService filePropertiesService;

  public FileRequestBodyAspect(FileService fileService, FilePropertiesService filePropertiesService) {
    this.fileService = fileService;
    this.filePropertiesService = filePropertiesService;
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

    filePropertiesService.getFileProperties(dto).forEach(
        f -> {
          var success = fileService.store(context.getRootBusinessProcessInstanceId(), f.getValue());
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
