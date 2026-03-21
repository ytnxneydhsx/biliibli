package com.bilibili.storage.image;

import com.bilibili.storage.common.StoredFile;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredFile saveImage(MultipartFile file, ImageStorageType imageStorageType);

    void deleteByPublicUrl(String publicUrl);
}
