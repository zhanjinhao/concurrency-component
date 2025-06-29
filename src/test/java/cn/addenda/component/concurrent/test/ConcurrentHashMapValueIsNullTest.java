package cn.addenda.component.concurrent.test;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author addenda
 * @since 2022/8/10
 */
public class ConcurrentHashMapValueIsNullTest {

  @Test
  public void main() {
    testNull();
    testComputeIfAbsent(new ConcurrentHashMap<>());
//    testComputeIfAbsent(new HashMap<>());
  }


  private static void testComputeIfAbsent(Map<String, String> map) {
    AtomicInteger atomicInteger = new AtomicInteger(0);

    final Thread thread1 = new Thread(() -> {
      for (int i = 0; i < 100000; i++) {
        map.computeIfAbsent(String.valueOf(i), s -> {
          atomicInteger.incrementAndGet();
          return "a";
        });
      }
    });
    thread1.start();

    final Thread thread2 = new Thread(() -> {
      for (int i = 0; i < 100000; i++) {
        map.computeIfAbsent(String.valueOf(i), s -> {
          atomicInteger.incrementAndGet();
          return "a";
        });
      }
    });
    thread2.start();

    try {
      thread1.join();
      thread2.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    System.out.println("expected value is 100000 , actually value is " + atomicInteger.get());
  }

  private static void testNull() {

    Map<String, String> map = new ConcurrentHashMap<>();

    map.computeIfAbsent("123", s -> "1");

    map.computeIfAbsent("1234", s -> null);

    System.out.println(map.get("123"));

    /**
     * value不能为null
     */
    try {
      map.put("234", null);
    } catch (RuntimeException e) {
      Assert.assertEquals(NullPointerException.class, e.getClass());
      if (!(e instanceof NullPointerException)) {
        throw e;
      }
    }
  }

}
