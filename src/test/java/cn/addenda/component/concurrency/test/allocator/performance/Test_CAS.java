package cn.addenda.component.concurrency.test.allocator.performance;

import cn.addenda.component.base.pojo.Binary;
import cn.addenda.component.concurrency.allocator.AllocatorException;
import cn.addenda.component.concurrency.allocator.lock.LockAllocator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author addenda
 * @since 2023/6/3 12:36
 */
public class Test_CAS extends Test_Base_Allocator {

  public Test_CAS() {
    super(new Impl_CAS());
  }

  @Test
  public void test() {
    // avg: 30
    baseTest();
  }

  public static class Impl_CAS implements LockAllocator<Lock> {

    private final Map<String, Binary<Lock, Integer>> lockMap = new HashMap<>();

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    public Lock allocate(String name) {
      while (!atomicInteger.compareAndSet(0, 1)) {
      }
      try {
        Binary<Lock, Integer> lockBinary = lockMap
                .computeIfAbsent(name, s -> Binary.of(new ReentrantLock(), 0));
        lockBinary.setF2(lockBinary.getF2() + 1);
        return lockBinary.getF1();
      } finally {
        while (!atomicInteger.compareAndSet(1, 0)) {
        }
      }

    }

    @Override
    public void release(String name) {
      while (!atomicInteger.compareAndSet(0, 1)) {
      }
      try {
        Binary<Lock, Integer> lockBinary = lockMap.get(name);
        if (lockBinary == null) {
          String msg = String.format("锁 [%s] 不存在！", name);
          throw new AllocatorException(msg);
        }

        lockBinary.setF2(lockBinary.getF2() - 1);
        int i = lockBinary.getF2();
        if (i == 0) {
          lockMap.remove(name);
        }
      } finally {
        while (!atomicInteger.compareAndSet(1, 0)) {
        }
      }
    }

    public Map<String, Binary<Lock, Integer>> getLockMap() {
      return lockMap;
    }
  }

}
