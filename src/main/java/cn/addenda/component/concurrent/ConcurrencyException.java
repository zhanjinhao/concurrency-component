package cn.addenda.component.concurrent;

import cn.addenda.component.base.BaseException;

/**
 * @author addenda
 * @since 2023/9/16 12:24
 */
public class ConcurrencyException extends BaseException {

  public ConcurrencyException() {
    super();
  }

  public ConcurrencyException(String message) {
    super(message);
  }

  public ConcurrencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConcurrencyException(Throwable cause) {
    super(cause);
  }

  public ConcurrencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public String moduleName() {
    return "concurrency";
  }

  @Override
  public String componentName() {
    return "concurrency";
  }
}
