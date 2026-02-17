package com.bilibili.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile saveAvatar(MultipartFile file);

    void deleteByPublicUrl(String publicUrl);
}
