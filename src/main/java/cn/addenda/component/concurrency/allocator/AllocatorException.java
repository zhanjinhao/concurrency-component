package cn.addenda.component.concurrency.allocator;

import cn.addenda.component.concurrency.ConcurrencyException;

/**
 * @author addenda
 * @since 2023/9/16 12:24
 */
public class AllocatorException extends ConcurrencyException {

  public AllocatorException() {
    super();
  }

  public AllocatorException(String message) {
    super(message);
  }

  public AllocatorException(String message, Throwable cause) {
    super(message, cause);
  }

  public AllocatorException(Throwable cause) {
    super(cause);
  }

  public AllocatorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public String moduleName() {
    return "concurrency";
  }

  @Override
  public String componentName() {
    return "allocator";
  }
}
