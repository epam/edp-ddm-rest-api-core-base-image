/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class ConstraintViolationException extends RequestProcessingException {
  public ConstraintViolationException(String message, String details) {
    super(message, Status.CONSTRAINT_VIOLATION, details);
  }

  public ConstraintViolationException(String message, Throwable cause, String details) {
    super(message, cause, Status.CONSTRAINT_VIOLATION, details);
  }
}
