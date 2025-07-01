package cn.addenda.component.concurrency.test.allocator.performance;

import cn.addenda.component.concurrency.allocator.lock.ReentrantLockAllocator;
import org.junit.Test;

/**
 * @author addenda
 * @since 2023/6/3 12:36
 */
public class Test_ReentrantLockAllocator extends Test_Base_Allocator {

  public Test_ReentrantLockAllocator() {
    super(new ReentrantLockAllocator());
  }

  @Test
  public void test() {
    // avg : 28
    baseTest();
  }

}
