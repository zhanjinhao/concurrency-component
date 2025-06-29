package cn.addenda.component.concurrent.test.allocator;

import cn.addenda.component.base.util.SleepUtils;
import cn.addenda.component.concurrent.allocator.lock.ReentrantLockIdleExpirationAllocator;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TestReentrantLockIdelExpirationAllocator {

  @Test
  public void test1() {
    ReentrantLockIdleExpirationAllocator allocator = new ReentrantLockIdleExpirationAllocator(100L, false);

    ReentrantLock a1 = allocator.allocateWithDefaultTtl("a");

    ReentrantLock a2 = allocator.allocateWithDefaultTtl("a");

    Assert.assertEquals(a1, a2);

    SleepUtils.sleep(TimeUnit.MILLISECONDS, 101);

    ReentrantLock a3 = allocator.allocate("a");

    Assert.assertNotEquals(a2, a3);
  }

}
