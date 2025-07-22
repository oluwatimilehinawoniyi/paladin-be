package com.paladin.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class FileUtils {
    public static String extractKeyFromUrl(String s3Url) {
        try {
            String[] parts = s3Url.split("/");
            // Join everything after the domain (parts[0] = https:, parts[1] = empty, parts[2] = domain)
            return String.join("/",
                    Arrays.copyOfRange(parts, 3, parts.length));
        } catch (Exception e) {
            log.error("Error extracting key from URL: {}", s3Url);
            throw new RuntimeException("Invalid S3 URL format");
        }
    }
}
