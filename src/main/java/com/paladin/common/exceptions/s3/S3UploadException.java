package com.paladin.common.exceptions.s3;

public class S3UploadException extends S3OperationException {
    public S3UploadException(String message) {
        super(message);
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
