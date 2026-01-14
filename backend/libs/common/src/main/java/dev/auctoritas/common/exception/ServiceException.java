package dev.auctoritas.common.exception;

public class ServiceException extends RuntimeException {
  private final String errorCode;
  private final String resourceId;
  private final String resourceType;

  public ServiceException(String message) {
    super(message);
    this.errorCode = "SERVICE_ERROR";
    this.resourceId = null;
    this.resourceType = null;
  }

  public ServiceException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.resourceId = null;
    this.resourceType = null;
  }

  public ServiceException(String message, String errorCode, String resourceId, String resourceType) {
    super(message);
    this.errorCode = errorCode;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
  }

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "SERVICE_ERROR";
    this.resourceId = null;
    this.resourceType = null;
  }

  public ServiceException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.resourceId = null;
    this.resourceType = null;
  }

  public ServiceException(String message, String errorCode, String resourceId, String resourceType, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public boolean hasResourceInfo() {
    return resourceId != null || resourceType != null;
  }
}
