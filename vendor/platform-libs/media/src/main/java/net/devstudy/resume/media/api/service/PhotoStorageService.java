package net.devstudy.resume.media.api.service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {
    /**
     * Stores profile photo and returns array {largeUrl, smallUrl}.
     */
    String[] store(MultipartFile file);
}
