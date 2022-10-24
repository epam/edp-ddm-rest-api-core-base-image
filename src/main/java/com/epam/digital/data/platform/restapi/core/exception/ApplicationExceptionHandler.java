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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.audit.AuditableException;
import com.epam.digital.data.platform.restapi.core.model.ConstraintErrorDetails;
import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Map<Status, String> externalErrorStatusToResponseCodeMap = Map.of(
      Status.THIRD_PARTY_SERVICE_UNAVAILABLE, ResponseCode.THIRD_PARTY_SERVICE_UNAVAILABLE,
      Status.PROCEDURE_ERROR, ResponseCode.PROCEDURE_ERROR,
      Status.INTERNAL_CONTRACT_VIOLATION, ResponseCode.INTERNAL_CONTRACT_VIOLATION,
      Status.JWT_EXPIRED, ResponseCode.JWT_EXPIRED,
      Status.JWT_INVALID, ResponseCode.JWT_INVALID,
      Status.FORBIDDEN_OPERATION, ResponseCode.FORBIDDEN_OPERATION);

  private final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

  private final TraceProvider traceProvider;

  public ApplicationExceptionHandler(TraceProvider traceProvider) {
    this.traceProvider = traceProvider;
  }

  @AuditableException
  @ExceptionHandler(CephCommunicationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleCephCommunicationException(
      Exception exception) {
    log.error("Exception while communication with ceph", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.THIRD_PARTY_SERVICE_UNAVAILABLE));
  }

  @AuditableException
  @ExceptionHandler(MisconfigurationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMisconfigurationException(
      Exception exception) {
    log.error("Ceph bucket not found", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.INTERNAL_CONTRACT_VIOLATION));
  }

  @ExceptionHandler(KafkaCephResponseNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleKafkaCephResponseNotFoundException(
      KafkaCephResponseNotFoundException exception) {
    log.error("Kafka response does not found in ceph", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.INTERNAL_CONTRACT_VIOLATION));
  }

  @AuditableException
  @ExceptionHandler(KepServiceInternalServerErrorException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleInternalServerErrorException(
      Exception exception) {
    log.error("External digital signature service has internal server error", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.THIRD_PARTY_SERVICE_UNAVAILABLE));
  }

  @AuditableException
  @ExceptionHandler(KepServiceBadRequestException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleBadRequestException(
      Exception exception) {
    log.error("Call to external digital signature service violates an internal contract",
        exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.INTERNAL_CONTRACT_VIOLATION));
  }

  @AuditableException(action = "SIGN_BREACH")
  @ExceptionHandler(InvalidSignatureException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleSignatureValidationException(
      InvalidSignatureException exception) {
    log.error("Digital signature validation failed", exception);
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .body(newDetailedResponse(ResponseCode.SIGNATURE_VIOLATION));
  }

  @AuditableException
  @ExceptionHandler(NoKafkaResponseException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleNoKafkaResponseExceptionException(
      NoKafkaResponseException exception) {
    log.error("No response from Kafka", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.TIMEOUT_ERROR));
  }

  @AuditableException(userInfoEnabled = false)
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAuthenticationException(
      AuthenticationException exception) {
    log.error("Authentication failure", exception);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(newDetailedResponse(ResponseCode.AUTHENTICATION_FAILED));
  }

  @AuditableException
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(ResponseCode.FORBIDDEN_OPERATION));
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    log.error("One or more input arguments are not valid", exception);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(getValidationErrorsResponse(exception.getBindingResult()));
  }

  @AuditableException
  @ExceptionHandler(DtoValidationException.class)
  protected ResponseEntity<Object> handleDtoValidationException(DtoValidationException exception) {
    log.error("Failed validation of input dto", exception);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(getValidationErrorsResponse(exception.getBindingResult()));
  }

  @AuditableException
  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.RUNTIME_ERROR));
  }

  @AuditableException
  @ExceptionHandler(SqlErrorException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleSqlErrorException(SqlErrorException exception) {
    log.error("sql exception occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.RUNTIME_ERROR));
  }

  @AuditableException
  @ExceptionHandler(ForbiddenOperationException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleForbiddenOperationException(ForbiddenOperationException exception) {
    log.error("User has invalid role", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(ResponseCode.FORBIDDEN_OPERATION));
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    var parseException = handleParseException(exception);
    if (parseException.isPresent()) {
      log.error("Can not read some of arguments", exception);
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(parseException.get());
    }

    log.error("Request body is not readable JSON", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.CLIENT_ERROR));
  }

  private Optional<DetailedErrorResponse<FieldsValidationErrorDetails>> handleParseException(
      HttpMessageNotReadableException exception) {
    if (exception.getCause() instanceof InvalidFormatException) {
      var ex = (InvalidFormatException) exception.getCause();

      var msg = ex.getOriginalMessage();
      if (ex.getCause() instanceof DateTimeParseException) {
        msg = ex.getCause().getMessage();
      }

      var value = String.valueOf(ex.getValue());

      var field = ex.getPath().stream()
          .map(Reference::getFieldName)
          .collect(joining("."));

      DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
          = newDetailedResponse(ResponseCode.VALIDATION_ERROR);

      var details = List.of(new FieldsValidationErrorDetails.FieldError(value, field, msg));
      invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(details));

      return Optional.of(invalidFieldsResponse);
    }

    return Optional.empty();
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("Payload format is in an unsupported format", ex);
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(newDetailedResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE));
  }

  @AuditableException
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      NoHandlerFoundException exception, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    log.error("Page not found", exception);
    return ResponseEntity.status(NOT_FOUND)
        .body(newDetailedResponse(ResponseCode.NOT_FOUND));
  }

  @AuditableException
  @ExceptionHandler(KafkaInternalServerException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleKafkaInternalException(
      KafkaInternalServerException kafkaInternalServerException) {
    log.error("Exceptional status in kafka response", kafkaInternalServerException);
    Response<?> kafkaResponse = kafkaInternalServerException.getKafkaResponse();
    String code =
        externalErrorStatusToResponseCodeMap
            .getOrDefault(kafkaResponse.getStatus(), ResponseCode.RUNTIME_ERROR);
    return ResponseEntity.status(kafkaInternalServerException.getHttpStatus())
        .body(newDetailedResponse(code));
  }

  @AuditableException
  @ExceptionHandler(DigitalSignatureNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleCephNoSuchObjectException(
      DigitalSignatureNotFoundException exception) {
    log.error("Digital signature not found", exception);
    DetailedErrorResponse<Void> responseBody =
        newDetailedResponse(ResponseCode.INVALID_HEADER_VALUE);
    return ResponseEntity.status(BAD_REQUEST).body(responseBody);
  }

  @AuditableException
  @ExceptionHandler(FileNotExistsException.class)
  public ResponseEntity<DetailedErrorResponse<List<String>>> handleFileNotExistsException(
      FileNotExistsException exception) {
    log.error("Some files were not found in ceph", exception);
    DetailedErrorResponse<List<String>> responseBody =
        newDetailedResponse(ResponseCode.FILE_NOT_FOUND);
    responseBody.setDetails(exception.getFieldsWithNotExistsFiles());
    return ResponseEntity.status(BAD_REQUEST)
        .body(responseBody);
  }

  @AuditableException
  @ExceptionHandler(KafkaConstraintViolationException.class)
  public ResponseEntity<DetailedErrorResponse<ConstraintErrorDetails>> handleKafkaConstraintException(
      KafkaConstraintViolationException exception) {
    log.error("Constraint violation occurred while processing", exception);
    Response<?> kafkaResponse = exception.getKafkaResponse();
    DetailedErrorResponse<ConstraintErrorDetails> responseBody =
        newDetailedResponse(ResponseCode.CONSTRAINT_ERROR);
    responseBody.setDetails(new ConstraintErrorDetails(kafkaResponse.getDetails()));
    return ResponseEntity.status(exception.getHttpStatus())
        .body(responseBody);
  }

  @AuditableException
  @ExceptionHandler(KafkaSecurityValidationFailedException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleKafkaSecurityException(
      KafkaSecurityValidationFailedException exception) {
    log.error("Request didn't pass one of security validations", exception);
    Response<?> kafkaResponse = exception.getKafkaResponse();
    String code = externalErrorStatusToResponseCodeMap.get(kafkaResponse.getStatus());
    return ResponseEntity.status(exception.getHttpStatus())
        .body(newDetailedResponse(code));
  }

  @AuditableException
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleNotFoundException(
      NotFoundException exception) {
    log.error("Resource not found", exception);
    return ResponseEntity.status(NOT_FOUND)
        .body(newDetailedResponse(ResponseCode.NOT_FOUND));
  }

  @AuditableException
  @ExceptionHandler(MandatoryHeaderMissingException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMandatoryHeaderMissingException(
      Exception exception) {
    log.error("Mandatory header(s) missed", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.HEADERS_ARE_MISSING));
  }

  @AuditableException
  @ExceptionHandler(MandatoryAccessTokenClaimMissingException.class)
  public ResponseEntity<DetailedErrorResponse<Void>>
  handleMandatoryAccessTokenClaimMissingException(Exception exception) {
    log.error("Mandatory access token claim(s) missed", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.INVALID_HEADER_VALUE));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleMethodArgumentTypeMismatchException(
      Exception exception) {
    log.error("Path argument is not valid", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(ResponseCode.METHOD_ARGUMENT_TYPE_MISMATCH));
  }

  @AuditableException
  @ExceptionHandler(ChecksumInconsistencyException.class)
  public ResponseEntity<DetailedErrorResponse<Void>> handleFileWasChangedException(
      ChecksumInconsistencyException exception) {
    log.error("File was changed between processing stages ", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(ResponseCode.FILE_WAS_CHANGED));
  }

  @Override
  protected ResponseEntity<Object> handleBindException(
      BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
    log.error("Request param is not readable", ex);

    var details = ex.getBindingResult().getAllErrors().stream()
        .map(this::bindErrorToFieldError)
        .collect(toList());

    DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse
        = newDetailedResponse(ResponseCode.CLIENT_ERROR);
    invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(details));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(invalidFieldsResponse);
  }

  private DetailedErrorResponse<FieldsValidationErrorDetails> getValidationErrorsResponse(
      BindingResult bindingResult) {
    DetailedErrorResponse<FieldsValidationErrorDetails> invalidFieldsResponse =
        newDetailedResponse(ResponseCode.VALIDATION_ERROR);

    var generalErrorList = bindingResult.getFieldErrors();
    var customErrorsDetails =
        generalErrorList.stream()
            .map(
                error ->
                    new FieldsValidationErrorDetails.FieldError(
                        String.valueOf(error.getRejectedValue()),
                        error.getField(),
                        error.getDefaultMessage()))
            .collect(toList());
    invalidFieldsResponse.setDetails(new FieldsValidationErrorDetails(customErrorsDetails));
    return invalidFieldsResponse;
  }

  private FieldsValidationErrorDetails.FieldError bindErrorToFieldError(ObjectError error) {
    String msg = error.getDefaultMessage();

    if (error instanceof FieldError) {
      var fieldError = (FieldError) error;

      if (fieldError.contains(TypeMismatchException.class)) {
        TypeMismatchException ex = fieldError.unwrap(TypeMismatchException.class);
        if (ex.getCause().getCause() instanceof IllegalArgumentException) {
          msg = ex.getCause().getCause().getMessage();
        }
      }

      return new FieldsValidationErrorDetails.FieldError(
          String.valueOf(fieldError.getRejectedValue()), fieldError.getField(), msg);
    } else {
      return new FieldsValidationErrorDetails.FieldError(msg);
    }
  }

  private <T> DetailedErrorResponse<T> newDetailedResponse(String code) {
    var response = new DetailedErrorResponse<T>();
    response.setTraceId(traceProvider.getRequestId());
    response.setCode(code);
    return response;
  }
}
