package cn.addenda.component.concurrency.thread.factory;

import cn.addenda.component.stacktrace.StackTraceUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

  private final String mPrefix;

  private final ThreadFactory threadFactory;

  private NamedThreadFactory(String mPrefix, ThreadFactory threadFactory) {
    this.mPrefix = mPrefix;
    this.threadFactory = threadFactory;
  }

  private final AtomicInteger mThreadNum = new AtomicInteger(1);

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = threadFactory.newThread(runnable);
    String originalThreadName = thread.getName();
    thread.setName(mPrefix + "-" + mThreadNum.getAndIncrement() + "-" + originalThreadName);
    return thread;
  }

  public static ThreadFactory of(String mPrefix, ThreadFactory threadFactory) {
    return new NamedThreadFactory(mPrefix, threadFactory);
  }

  public static ThreadFactory of(ThreadFactory threadFactory) {
    return new NamedThreadFactory(StackTraceUtils.getCallerInfo(), threadFactory);
  }

}
