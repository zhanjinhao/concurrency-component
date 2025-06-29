package cn.addenda.component.concurrent.allocator;

import cn.addenda.component.base.AbstractNamed;
import cn.addenda.component.base.pojo.Binary;
import cn.addenda.component.concurrent.allocator.factory.DumbLockFactory;
import cn.addenda.component.concurrent.allocator.factory.LockFactory;
import cn.addenda.component.concurrent.allocator.factory.ReentrantLockFactory;
import cn.addenda.component.concurrent.allocator.factory.ReentrantSegmentLockFactory;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/5/30 22:51
 */
@ToString(exclude = {"lockFactory"})
public abstract class ReferenceCountAllocator<T> extends AbstractNamed implements Allocator<T> {

  private final Map<String, Binary<T, AtomicInteger>> map = new ConcurrentHashMap<>();

  /**
   * lockFactory的作用是生产lock并且在执行{@link ReferenceCountAllocator#allocate(String)}和{@link ReferenceCountAllocator#release(String)}
   * 的时候保证并发安全。
   *
   * <ul>
   *   <li>如果不需要保证并发安全，使用{@link DumbLockFactory}</li>
   *   <li>如果需要保证全局并发安全，使用{@link ReentrantLockFactory}</li>
   *   <li>如果需要保证分段并发安全（相比于全局并发安全，可以提升性能），使用{@link ReentrantSegmentLockFactory}</li>
   * </ul>
   */
  private final LockFactory<String> lockFactory;

  protected ReferenceCountAllocator(LockFactory<String> lockFactory) {
    this.lockFactory = lockFactory;
  }

  @Override
  public T allocate(String name) {
    Lock lock = lockFactory.getLock(name);
    lock.lock();
    try {
      Binary<T, AtomicInteger> binary = map
              .computeIfAbsent(name, s -> Binary.of(referenceFunction().apply(name), new AtomicInteger(0)));
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

  /**
   * 如果{@link ReferenceCountAllocator#map}里没有与name绑定的对象，通过此函数申请对象。
   * Function的参数是资源名称，返回值是资源。
   */
  protected abstract Function<String, T> referenceFunction();

}
