package dev.auctoritas.common.exception;

/**
 * Base exception for all Auctoritas business exceptions.
 */
public class AuctoritasException extends RuntimeException {
  private final String errorCode;

  public AuctoritasException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public AuctoritasException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
