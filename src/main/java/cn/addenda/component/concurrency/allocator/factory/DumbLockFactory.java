package cn.addenda.component.concurrency.allocator.factory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class DumbLockFactory<T> implements LockFactory<T> {

  private static final Lock LOCK = new Lock() {

    @Override
    public void lock() {
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
      return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException();
    }

  };

  @Override
  public Lock getLock(T t) {
    return LOCK;
  }
}
