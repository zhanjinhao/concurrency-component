package cn.addenda.component.concurrent.allocator;

import cn.addenda.component.base.Named;

import java.util.UUID;

/**
 * 此接口的作用是从资源池分配/释放一个对象。至于资源池里存的是什么由具体的实现确定。
 *
 * @author addenda
 * @since 2023/9/1 8:59
 */
public interface Allocator<T> extends Named {

  /**
   * 分配一个对象并且后续的分配都获取的是此对象，除非调用{@link Allocator#release(String)}。
   */
  T allocate(String name);

  /**
   * 释放一个对象。
   */
  void release(String name);

  @Override
  default String getName() {
    return UUID.randomUUID().toString().replace("-", "");
  }

}
