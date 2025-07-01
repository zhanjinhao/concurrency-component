package cn.addenda.component.concurrency.allocator.lock;

import cn.addenda.component.concurrency.allocator.ReferenceCountIdleExpirationAllocator;
import cn.addenda.component.concurrency.allocator.factory.ReentrantSegmentLockFactory;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ReentrantLockIdleExpirationAllocator
        extends ReferenceCountIdleExpirationAllocator<ReentrantLock>
        implements LockAllocator<ReentrantLock> {

  public ReentrantLockIdleExpirationAllocator(long ttl, boolean alwaysUpdateTtl) {
    super(new ReentrantSegmentLockFactory(), ttl, alwaysUpdateTtl);
  }

  @Override
  protected Function<String, ReentrantLock> referenceFunction() {
    return s -> new ReentrantLock();
  }

}
