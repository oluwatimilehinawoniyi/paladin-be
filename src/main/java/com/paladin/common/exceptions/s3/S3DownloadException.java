package com.paladin.common.exceptions.s3;

public class S3DownloadException extends S3OperationException {
    public S3DownloadException(String message) {
        super(message);
    }

    public S3DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}