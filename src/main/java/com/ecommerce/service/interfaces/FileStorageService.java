package com.ecommerce.service.interfaces;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    
    String storeFile(MultipartFile file);
    
    Resource loadFileAsResource(String fileName);
} 