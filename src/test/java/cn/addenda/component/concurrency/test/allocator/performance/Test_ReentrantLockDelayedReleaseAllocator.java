package cn.addenda.component.concurrency.test.allocator.performance;

import cn.addenda.component.concurrency.allocator.lock.ReentrantLockDelayedReleaseAllocator;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/6/3 12:36
 */
public class Test_ReentrantLockDelayedReleaseAllocator extends Test_Base_Allocator {

  public Test_ReentrantLockDelayedReleaseAllocator() {
    super(new ReentrantLockDelayedReleaseAllocator(1000L));
  }

  @Test
  public void test() {
    baseTest();
  }

}
