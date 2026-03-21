package com.bilibili.storage.multipart;

import com.bilibili.upload.video.model.dto.VideoUploadPartETagDTO;

import java.util.List;
import java.util.Map;

public interface MultipartObjectStorageService {

    long getMaxObjectSize();

    int getChunkSize();

    boolean isAllowedContentType(String contentType);

    boolean isAllowedFileName(String originalFileName);

    String buildObjectKey(Long uid, String originalFileName, String contentType);

    String buildPublicUrl(String objectKey);

    String createMultipartUpload(String objectKey, String contentType);

    Map<Integer, String> signUploadPartUrls(String objectKey, String multipartUploadId, List<Integer> partNumbers);

    List<Integer> listUploadedParts(String objectKey, String multipartUploadId);

    void completeMultipartUpload(String objectKey, String multipartUploadId, List<VideoUploadPartETagDTO> parts);

    void abortMultipartUpload(String objectKey, String multipartUploadId);

    void deleteObject(String objectKey);
}
