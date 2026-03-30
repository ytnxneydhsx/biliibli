package com.bilibili.im.upload.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImImageUploadService {

    String uploadImage(Long uid, MultipartFile file);
}
