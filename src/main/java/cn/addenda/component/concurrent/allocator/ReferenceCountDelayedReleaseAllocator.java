package cn.addenda.component.concurrent.allocator;

import cn.addenda.component.base.AbstractNamed;
import cn.addenda.component.base.pojo.Binary;
import cn.addenda.component.concurrent.allocator.factory.DumbLockFactory;
import cn.addenda.component.concurrent.allocator.factory.LockFactory;
import cn.addenda.component.concurrent.allocator.factory.ReentrantLockFactory;
import cn.addenda.component.concurrent.allocator.factory.ReentrantSegmentLockFactory;
import lombok.Setter;
import lombok.ToString;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/5/30 22:51
 */
@ToString(exclude = {"lockFactory"})
public abstract class ReferenceCountDelayedReleaseAllocator<T>
        extends AbstractNamed
        implements DelayedReleaseAllocator<T> {

  private final Map<String, Binary<T, AtomicInteger>> map = new ConcurrentHashMap<>();

  private final Map<String, Binary<T, Long>> releasedMap = new ConcurrentHashMap<>();

  private final Long delayReleaseTtl;

  @Setter
  private long cleaningFrequency = 100;

  private final AtomicLong count = new AtomicLong(0L);

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

  protected ReferenceCountDelayedReleaseAllocator(LockFactory<String> lockFactory, Long delayReleaseTtl) {
    this.lockFactory = lockFactory;
    this.delayReleaseTtl = delayReleaseTtl;
  }

  @Override
  public void delayRelease(String name) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      Binary<T, AtomicInteger> binary = map.get(name);
      if (binary == null) {
        String msg = String.format("资源 [%s] 不存在！", name);
        throw new AllocatorException(msg);
      }
      int i = binary.getF2().decrementAndGet();
      if (i == 0) {
        i = binary.getF2().get();
        if (i == 0) {
          Binary<T, AtomicInteger> removed = map.remove(name);
          releasedMap.put(name, Binary.of(removed.getF1(), System.currentTimeMillis() + delayReleaseTtl));
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public T allocate(String name) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      clear();

      Binary<T, AtomicInteger> binary = map
              .computeIfAbsent(name, new Function<String, Binary<T, AtomicInteger>>() {
                @Override
                public Binary<T, AtomicInteger> apply(String s) {
                  Binary<T, Long> releasedT = releasedMap.remove(s);
                  if (releasedT != null && System.currentTimeMillis() > releasedT.getF2()) {
                    releasedT = null;
                  }
                  if (releasedT == null) {
                    return Binary.of(referenceFunction().apply(name), new AtomicInteger(0));
                  }
                  return Binary.of(releasedT.getF1(), new AtomicInteger(0));
                }
              });
      binary.getF2().getAndIncrement();
      return binary.getF1();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void release(String name) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      Binary<T, AtomicInteger> binary = map.get(name);
      if (binary == null) {
        String msg = String.format("资源 [%s] 不存在！", name);
        throw new AllocatorException(msg);
      }
      int i = binary.getF2().decrementAndGet();
      if (i == 0) {
        map.remove(name);
      }
    } finally {
      lock.unlock();
    }
  }

  private void clear() {
    long c = count.incrementAndGet();
    if (c % cleaningFrequency != 0) {
      return;
    }
    long now = System.currentTimeMillis();
    Iterator<Map.Entry<String, Binary<T, Long>>> iterator = releasedMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Binary<T, Long>> next = iterator.next();
      Long expire = next.getValue().getF2();
      if (now > expire) {
        iterator.remove();
      }
    }
  }

  /**
   * 如果{@link ReferenceCountDelayedReleaseAllocator#map}里没有与name绑定的对象，通过此函数申请对象。
   * Function的参数是资源名称，返回值是资源。
   */
  protected abstract Function<String, T> referenceFunction();

}
