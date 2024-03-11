package cat.joanpujol.smasolar.error;

/** An unnexpected condition aka bug */
public class UnnexpectedCodition extends RuntimeException {
  public UnnexpectedCodition() {}

  public UnnexpectedCodition(String message) {
    super(message);
  }

  public UnnexpectedCodition(String message, Throwable cause) {
    super(message, cause);
  }

  public UnnexpectedCodition(Throwable cause) {
    super(cause);
  }

  public UnnexpectedCodition(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
