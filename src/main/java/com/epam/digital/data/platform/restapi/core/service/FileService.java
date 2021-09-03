package com.epam.digital.data.platform.restapi.core.service;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExclusiveException;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class FileService {

  private final Logger log = LoggerFactory.getLogger(FileService.class);

  private final boolean isFileProcessingEnabled;
  private final String lowcodeFileBucket;
  private final String datafactoryFileBucket;

  private final CephService lowcodeFileCephService;
  private final CephService datafactoryFileCephService;

  public FileService(
      @Value("${data-platform.files.processing.enabled}") boolean isFileProcessingEnabled,
      @Value("${lowcode-file-ceph.bucket}") String lowcodeFileBucket,
      @Value("${datafactory-file-ceph.bucket}") String datafactoryFileBucket,
      @Qualifier("lowcodeFileCephService") CephService lowcodeFileCephService,
      @Qualifier("datafactoryFileCephService") CephService datafactoryFileCephService) {
    this.isFileProcessingEnabled = isFileProcessingEnabled;
    this.lowcodeFileBucket = lowcodeFileBucket;
    this.datafactoryFileBucket = datafactoryFileBucket;
    this.lowcodeFileCephService = lowcodeFileCephService;
    this.datafactoryFileCephService = datafactoryFileCephService;
  }

  public boolean store(String instanceId, File file) {
    if (isFileProcessingEnabled) {
      log.info("Storing file from lowcode to data ceph bucket");

      var lowcodeId = createCompositeObjectId(instanceId, file.getId());

      var cephResponseOpt = lowcodeFileCephService.getObject(lowcodeFileBucket, lowcodeId);
      if (cephResponseOpt.isEmpty()) {
        return false;
      }
      var cephResponse = cephResponseOpt.get();

      if (!isFileChecksumEqualsToCephCalculated(cephResponse.getContent(), file)) {
        throw new ChecksumInconsistencyException("Checksum from ceph and from request do not match");
      }

      if (!datafactoryFileCephService.doesObjectExist(datafactoryFileBucket, file.getId())) {
        datafactoryFileCephService.putObject(datafactoryFileBucket, file.getId(), cephResponse);
      } else {
        throw new FileNotExclusiveException(
            "Datafactory file bucket already contain file with id: " + file.getId());
      }
    }

    return true;
  }

  public List<FileProperty> getFileProperties(Object obj) {
    Collection<Object> objs;

    if (Collection.class.isAssignableFrom(obj.getClass())) {
      objs = (Collection<Object>) obj;
    } else {
      objs = singletonList(obj);
    }

    return getFileProperties(objs);
  }

  private List<FileProperty> getFileProperties(Collection<Object> objs) {
    return objs.stream()
        .flatMap(o ->
            Arrays.stream(o.getClass().getDeclaredFields())
                .filter(f -> File.class.equals(f.getType()))
                .peek(ReflectionUtils::makeAccessible)
                .map(f -> {
                  var value = (File) ReflectionUtils.getField(f, o);
                  if (value != null) {
                    return new FileProperty(f.getName(), value);
                  } else {
                    return null;
                  }
                })
                .filter(Objects::nonNull)
        )
        .collect(toList());
  }

  public boolean retrieve(String instanceId, File file) {
    if (isFileProcessingEnabled) {
      log.info("Storing file from data to lowcode ceph bucket");

      var cephResponseOpt =
          datafactoryFileCephService.getObject(datafactoryFileBucket, file.getId());
      if (cephResponseOpt.isEmpty()) {
        return false;
      }
      var cephResponse = cephResponseOpt.get();

      if (!isFileChecksumEqualsToCephCalculated(cephResponse.getContent(), file)) {
        throw new ChecksumInconsistencyException(
            "Checksum from ceph and from retrieved file object do not match");
      }

      var lowcodeId = createCompositeObjectId(instanceId, file.getId());
      lowcodeFileCephService.putObject(lowcodeFileBucket, lowcodeId, cephResponse);
    }

    return true;
  }

  private String createCompositeObjectId(String instanceId, String objectId) {
    return "process/" + instanceId +"/" + objectId;
  }

  private boolean isFileChecksumEqualsToCephCalculated(byte[] cephContent, File file) {
    var checksumFromFile = file.getChecksum();
    var checksumFromCephContent = DigestUtils.sha256Hex(cephContent);
    return StringUtils.equals(checksumFromCephContent, checksumFromFile);
  }
}
