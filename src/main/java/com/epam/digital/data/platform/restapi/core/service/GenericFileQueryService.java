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

package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.model.core.file.FileResponseDto;
import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.exception.NotFoundException;
import com.epam.digital.data.platform.restapi.core.model.FileRequestDto;
import com.epam.digital.data.platform.restapi.core.queryhandler.QueryHandler;
import com.epam.digital.data.platform.restapi.core.utils.FileUtils;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public abstract class GenericFileQueryService<I, O> {

  private final QueryHandler<I, O> queryHandler;
  @Autowired
  private FileService fileService;

  public GenericFileQueryService(QueryHandler<I, O> queryHandler) {
    this.queryHandler = queryHandler;
  }

  public Response<FileResponseDto> requestDto(Request<FileRequestDto<I>> input) {
    var entityId = input.getPayload().getEntityId();
    var requestedFileId = input.getPayload().getFileId();
    var request = new Request<>(entityId, input.getRequestContext(), input.getSecurityContext());

    var response = new Response<FileResponseDto>();

    var result = queryHandler.findById(request);
    if (result.isPresent()) {
      var fileFieldValueOpt = getFileFieldById(result.get(), requestedFileId);
      if (fileFieldValueOpt.isPresent()) {
        var fileFieldValue = fileFieldValueOpt.get();
        var fileDataDto = fileService.retrieve(fileFieldValueOpt.get());
        var fileContent = FileUtils.getByteContentFromStream(fileDataDto.getContent());
        var base64EncodedFileContent = Base64.getEncoder().encodeToString(fileContent);
        var fileResponseDto =
                new FileResponseDto(
                        base64EncodedFileContent,
                        fileFieldValue.getChecksum(),
                        fileDataDto.getMetadata().getFilename());
        response.setPayload(fileResponseDto);
        response.setStatus(Status.SUCCESS);
      } else {
        throw new FileNotExistsException("Entity in db doesn't contain file with id ", List.of(requestedFileId));
      }
    } else {
      response.setStatus(Status.NOT_FOUND);
    }
    return response;
  }

  public abstract Optional<File> getFileFieldById(O entity, String requestedFileId);

  public Response<FileDataDto> requestFile(Request<FileRequestDto<I>> input) {
    var entityId = input.getPayload().getEntityId();
    var requestedFileId = input.getPayload().getFileId();
    var request = new Request<>(entityId, input.getRequestContext(), input.getSecurityContext());

    var response = new Response<FileDataDto>();

    var result = queryHandler.findById(request);
    if (result.isPresent()) {
      var fileFieldValue = getFileFieldById(result.get(), requestedFileId);
      if (fileFieldValue.isPresent()) {
        var fileDataDto = fileService.retrieve(fileFieldValue.get());
        response.setPayload(fileDataDto);
        response.setStatus(Status.SUCCESS);
      } else {
        throw new FileNotExistsException("Entity in db doesn't contain file with id ", List.of(requestedFileId));
      }
    } else {
      throw new NotFoundException("Entity with file is not found in db");
    }
    return response;

  }
}
