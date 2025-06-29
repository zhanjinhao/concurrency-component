package cn.addenda.component.concurrent.allocator.lock;

import cn.addenda.component.concurrent.allocator.ReferenceCountDelayedReleaseAllocator;
import cn.addenda.component.concurrent.allocator.factory.ReentrantSegmentLockFactory;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/5/30 22:51
 */
public class ReentrantLockDelayedReleaseAllocator
        extends ReferenceCountDelayedReleaseAllocator<ReentrantLock>
        implements LockAllocator<ReentrantLock> {

  public ReentrantLockDelayedReleaseAllocator(Long delayReleaseTtl) {
    super(new ReentrantSegmentLockFactory(), delayReleaseTtl);
  }

  @Override
  protected Function<String, ReentrantLock> referenceFunction() {
    return s -> new ReentrantLock();
  }

}
