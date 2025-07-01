package cn.addenda.component.concurrency.test.allocator.performance;

import cn.addenda.component.concurrency.allocator.lock.ReentrantLockIdleExpirationAllocator;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/6/3 12:36
 */
public class Test_ReentrantLockIdleExpirationAllocator extends Test_Base_Allocator {

  public Test_ReentrantLockIdleExpirationAllocator() {
    super(new ReentrantLockIdleExpirationAllocator(1L, false));
  }

  @Test
  public void test() {
    baseTest();
  }

}
