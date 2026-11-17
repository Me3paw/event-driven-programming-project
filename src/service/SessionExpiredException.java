package service;

public final class SessionExpiredException extends SecurityException {
  private static final long serialVersionUID = 1L;
  public SessionExpiredException(String message) { super(message); }
}
