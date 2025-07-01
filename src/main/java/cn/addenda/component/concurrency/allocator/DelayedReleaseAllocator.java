package cn.addenda.component.concurrency.allocator;

public interface DelayedReleaseAllocator<T> extends Allocator<T> {

  /**
   * 延迟释放资源
   */
  void delayRelease(String name);

}
