package com.paladin.common.exceptions.s3;

public class S3DeleteException extends S3OperationException {
    public S3DeleteException(String message) {
        super(message);
    }

    public S3DeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}