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

package com.epam.digital.data.platform.restapi.core.utils;

public final class ResponseCode {

  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
  public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
  public static final String SIGNATURE_VIOLATION = "SIGNATURE_VIOLATION";
  public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String RUNTIME_ERROR = "RUNTIME_ERROR";
  public static final String THIRD_PARTY_SERVICE_UNAVAILABLE = "THIRD_PARTY_SERVICE_UNAVAILABLE";
  public static final String INTERNAL_CONTRACT_VIOLATION = "INTERNAL_CONTRACT_VIOLATION";
  public static final String PROCEDURE_ERROR = "PROCEDURE_ERROR";
  public static final String CONSTRAINT_ERROR = "CONSTRAINT_ERROR";
  public static final String CLIENT_ERROR = "CLIENT_ERROR";
  public static final String HEADERS_ARE_MISSING = "HEADERS_ARE_MISSING";
  public static final String INVALID_HEADER_VALUE = "INVALID_HEADER_VALUE";
  public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
  public static final String METHOD_ARGUMENT_TYPE_MISMATCH = "METHOD_ARGUMENT_TYPE_MISMATCH";
  public static final String JWT_INVALID = "JWT_INVALID";
  public static final String JWT_EXPIRED = "JWT_EXPIRED";
  public static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  public static final String FILE_WAS_CHANGED = "FILE_WAS_CHANGED";

  private ResponseCode() {
  }
}