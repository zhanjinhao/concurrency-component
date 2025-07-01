package cn.addenda.component.concurrency.allocator;

import java.util.concurrent.TimeUnit;

/**
 * @author addenda
 * @since 2023/9/12 20:58
 */
public interface IdelExpirationAllocator<T> extends Allocator<T> {

  /**
   * 分配一个对象并且后续的分配都获取的是此对象，
   * 除非调用{@link Allocator#release(String)} 或达到默认的的过期时间。
   */
  T allocateWithDefaultTtl(String name);

  /**
   * 分配一个对象并且后续的分配都获取的是此对象，
   * 除非调用{@link Allocator#release(String)} 或达到指定的的过期时间。
   */
  T allocate(String name, TimeUnit timeUnit, long ttl);

}
