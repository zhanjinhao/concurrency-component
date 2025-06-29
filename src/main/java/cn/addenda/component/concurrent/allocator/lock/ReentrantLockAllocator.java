package cn.addenda.component.concurrent.allocator.lock;

import cn.addenda.component.concurrent.allocator.ReferenceCountAllocator;
import cn.addenda.component.concurrent.allocator.factory.ReentrantSegmentLockFactory;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/5/30 22:51
 */
public class ReentrantLockAllocator
        extends ReferenceCountAllocator<ReentrantLock>
        implements LockAllocator<ReentrantLock> {

  public ReentrantLockAllocator() {
    super(new ReentrantSegmentLockFactory());
  }

  @Override
  protected Function<String, ReentrantLock> referenceFunction() {
    return s -> new ReentrantLock();
  }

}
