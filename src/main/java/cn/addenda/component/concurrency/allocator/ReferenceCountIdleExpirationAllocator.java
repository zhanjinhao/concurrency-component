package cn.addenda.component.concurrency.allocator;

import cn.addenda.component.base.AbstractNamed;
import cn.addenda.component.base.pojo.Ternary;
import cn.addenda.component.concurrency.allocator.factory.DumbLockFactory;
import cn.addenda.component.concurrency.allocator.factory.LockFactory;
import cn.addenda.component.concurrency.allocator.factory.ReentrantLockFactory;
import cn.addenda.component.concurrency.allocator.factory.ReentrantSegmentLockFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/9/13 21:35
 */
public abstract class ReferenceCountIdleExpirationAllocator<T>
        extends AbstractNamed
        implements IdelExpirationAllocator<T> {

  /**
   * key：名字
   * value: T,对象；Long,过期时间
   */
  private final Map<String, Ternary<T, AtomicInteger, Long>> map = new ConcurrentHashMap<>();

  /**
   * lockFactory的作用是生产lock并且在执行{@link ReferenceCountDelayedReleaseAllocator#allocate(String)}和{@link ReferenceCountDelayedReleaseAllocator#release(String)}
   * 的时候保证并发安全。
   *
   * <ul>
   *   <li>如果不需要保证并发安全，使用{@link DumbLockFactory}</li>
   *   <li>如果需要保证全局并发安全，使用{@link ReentrantLockFactory}</li>
   *   <li>如果需要保证分段并发安全（相比于全局并发安全，可以提升性能），使用{@link ReentrantSegmentLockFactory}</li>
   * </ul>
   */
  private final LockFactory<String> lockFactory;

  private final long ttl;

  private final boolean alwaysUpdateTtl;

  protected ReferenceCountIdleExpirationAllocator(LockFactory<String> lockFactory, long ttl, boolean alwaysUpdateTtl) {
    this.lockFactory = lockFactory;
    this.ttl = ttl;
    this.alwaysUpdateTtl = alwaysUpdateTtl;
  }

  @Override
  public T allocate(String name) {
    return allocate(name, TimeUnit.DAYS, 3650);
  }

  @Override
  public T allocateWithDefaultTtl(String name) {
    return allocate(name, TimeUnit.MILLISECONDS, ttl);
  }

  @Override
  public void release(String name) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      Ternary<T, AtomicInteger, Long> removed = map.get(name);
      if (removed == null) {
        String msg = String.format("资源 [%s] 不存在！", name);
        throw new AllocatorException(msg);
      }
      int i = removed.getF2().decrementAndGet();
      if (i == 0) {
        map.remove(name);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public T allocate(String name, TimeUnit timeUnit, long timeout) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      long now = System.currentTimeMillis();
      long expire = timeUnit.toMillis(timeout) + now;
      Ternary<T, AtomicInteger, Long> oldTernary = map.remove(name);
      // 如果之前不存在，存数据及ttl
      if (oldTernary == null) {
        T apply = referenceFunction().apply(name);
        map.put(name, Ternary.of(apply, new AtomicInteger(1), expire));
        return apply;
      }
      // 如果之前存在
      else {
        AtomicInteger f2 = oldTernary.getF2();
        f2.incrementAndGet();
        long oldExpire = oldTernary.getF3();
        T apply;
        if (now > oldExpire) {
          // ttl过期了： 新建一个对象替换旧对象，并更新ttl
          apply = referenceFunction().apply(name);
          map.put(name, Ternary.of(apply, f2, expire));
        } else {
          // ttl没有过期
          apply = oldTernary.getF1();
          map.put(name, Ternary.of(apply, f2, alwaysUpdateTtl ? expire : oldExpire));
        }
        return apply;
      }
    } finally {
      lock.unlock();
    }
  }

  protected abstract Function<String, T> referenceFunction();

}
