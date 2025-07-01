package cn.addenda.component.concurrency.test.allocator;

import cn.addenda.component.base.util.SleepUtils;
import cn.addenda.component.concurrency.allocator.lock.ReentrantLockDelayedReleaseAllocator;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TestReentrantLockDelayedReleaseAllocator {

  @Test
  public void test1() {
    ReentrantLockDelayedReleaseAllocator allocator = new ReentrantLockDelayedReleaseAllocator(100L);

    ReentrantLock a1 = allocator.allocate("a");

    allocator.delayRelease("a");

    ReentrantLock a2 = allocator.allocate("a");

    Assert.assertEquals(a1, a2);

    allocator.delayRelease("a");

    SleepUtils.sleep(TimeUnit.MILLISECONDS, 101);

    ReentrantLock a3 = allocator.allocate("a");

    Assert.assertNotEquals(a2, a3);
  }

}
