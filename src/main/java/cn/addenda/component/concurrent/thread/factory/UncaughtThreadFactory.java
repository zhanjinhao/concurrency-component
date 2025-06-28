package cn.addenda.component.concurrent.thread.factory;

import cn.addenda.component.stacktrace.StackTraceUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

@Slf4j
public class UncaughtThreadFactory implements ThreadFactory {

  private final String caller;

  private final ThreadFactory threadFactory;

  private UncaughtThreadFactory(String caller, ThreadFactory threadFactory) {
    this.caller = caller;
    this.threadFactory = threadFactory;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = threadFactory.newThread(runnable);
    Thread.UncaughtExceptionHandler oldUncaughtExceptionHandler = thread.getUncaughtExceptionHandler();

    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        log.error("an exception occurred, the caller of the factory is [{}], thread is [{}]. ", caller, t, e);
        if (oldUncaughtExceptionHandler != null) {
          oldUncaughtExceptionHandler.uncaughtException(t, e);
        }
      }
    });
    return thread;
  }

  public static ThreadFactory of(String caller, ThreadFactory threadFactory) {
    return new UncaughtThreadFactory(caller, threadFactory);
  }

  public static ThreadFactory of(ThreadFactory threadFactory) {
    return new UncaughtThreadFactory(StackTraceUtils.getCallerInfo(), threadFactory);
  }

}
