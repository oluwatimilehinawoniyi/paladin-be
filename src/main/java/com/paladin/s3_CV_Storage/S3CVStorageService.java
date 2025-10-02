package com.paladin.s3_CV_Storage;

import com.paladin.common.exceptions.s3.S3DeleteException;
import com.paladin.common.exceptions.s3.S3DownloadException;
import com.paladin.common.exceptions.s3.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3CVStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file,
                             String key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize())
            );

            return String.format(
                    "https://%s.s3.%s.amazonaws.com/%s",
                    bucketName,
                    s3Client.serviceClientConfiguration()
                            .region().id(),
                    key
            );

        } catch (IOException e) {
            log.error("IO error during S3 upload of file {}: {}", key,
                    e.getMessage(), e);
            throw new S3UploadException(
                    "Failed to read file data for upload.", e);
        } catch (S3Exception e) {
            log.error(
                    "S3 service error during upload of file {}: Code={}, Message={}",
                    key, e.statusCode(),
                    e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException(
                    "S3 service error during file upload: " + e.awsErrorDetails()
                            .errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during S3 upload of file {}: {}",
                    key, e.getMessage(), e);
            throw new S3UploadException(
                    "An unexpected error occurred during file upload.", e);
        }
    }

    public byte[] downloadFile(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(request)
                    .readAllBytes();
        } catch (S3Exception e) {
            log.error(
                    "S3 service error during download of file {}: Code={}, Message={}",
                    key, e.statusCode(),
                    e.awsErrorDetails().errorMessage(), e);
            throw new S3DownloadException(
                    "S3 service error during file download: " + e.awsErrorDetails()
                            .errorMessage(), e);
        } catch (IOException e) {
            log.error("IO error during S3 download of file {}: {}", key,
                    e.getMessage(), e);
            throw new S3DownloadException(
                    "Failed to read downloaded file data.", e);
        } catch (Exception e) {
            log.error("Unexpected error during S3 download of file {}: {}",
                    key, e.getMessage(), e);
            throw new S3DownloadException(
                    "An unexpected error occurred during file download.",
                    e);
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest =
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            log.error(
                    "S3 service error during deletion of file {}: Code={}, Message={}",
                    key, e.statusCode(),
                    e.awsErrorDetails().errorMessage(), e);
            throw new S3DeleteException(
                    "S3 service error during file deletion: " + e.awsErrorDetails()
                            .errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during S3 deletion of file {}: {}",
                    key, e.getMessage(), e);
            throw new S3DeleteException(
                    "An unexpected error occurred during file deletion.",
                    e);
        }
    }

}
