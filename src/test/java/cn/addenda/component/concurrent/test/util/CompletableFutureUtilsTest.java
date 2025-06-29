package cn.addenda.component.concurrent.test.util;

import cn.addenda.component.base.lambda.wrapper.CostedSupplier;
import cn.addenda.component.base.lambda.wrapper.NamedRunnable;
import cn.addenda.component.base.util.SleepUtils;
import cn.addenda.component.concurrent.thread.factory.NamedThreadFactory;
import cn.addenda.component.concurrent.util.CompletableFutureUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author addenda
 * @since 2023/10/3 9:26
 */
@Slf4j
public class CompletableFutureUtilsTest {

  @Test
  public void test1() {

    AtomicReference<String> a1 = new AtomicReference<>(null);
    AtomicReference<String> a2 = new AtomicReference<>(null);
    AtomicReference<String> a3 = new AtomicReference<>(null);
    AtomicReference<String> a4 = new AtomicReference<>(null);
    AtomicReference<String> a5 = new AtomicReference<>(null);

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            2,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            NamedThreadFactory.of("CompletableFutureUtilsTest", Executors.defaultThreadFactory()));

    log.info("start ");
    CompletableFuture<Void> f1 = CompletableFuture.runAsync(NamedRunnable.of("f1", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 3);
      a1.set("a1");
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f1, 1, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f1" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f2 = CompletableFuture.runAsync(NamedRunnable.of("f2", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 3);
      a2.set("a2");
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f2, 1, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f2" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f3 = CompletableFuture.runAsync(NamedRunnable.of("f3", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 3);
      a3.set("a3");
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f3, 1, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f3" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f4 = CompletableFuture.runAsync(NamedRunnable.of("f4", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 3);
      a4.set("a4");
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f4, 1, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f4" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f5 = CompletableFuture.runAsync(NamedRunnable.of("f5", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 3);
      a5.set("a5");
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f5, 1, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f5" + throwable.toString());
              return null;
            });

    Thread monitorThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          SleepUtils.sleep(TimeUnit.MILLISECONDS, 200);
          log.info(threadPoolExecutor.toString());
        }
      }
    });
    monitorThread.setDaemon(true);
    monitorThread.start();

    // 结论：
    // 1、超时后，正在执行中的任务会执行完成，排队的任务不会被线程执行
    // 2、排队中的任务被取消的原理：任务里设置了标志位。这些任务依然会被从Queue里取出来，但是不会执行我们设置的逻辑。这些任务依然被认为是completed tasks。
    SleepUtils.sleep(TimeUnit.SECONDS, 5);

    Assert.assertEquals("a1", a1.get());
    Assert.assertEquals("a2", a2.get());
    Assert.assertNull(a3.get());
    Assert.assertNull(a4.get());
    Assert.assertNull(a5.get());
  }

  @Test
  public void test2() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(1);

    Supplier<Object> supplier = new Supplier<Object>() {
      @Override
      public Object get() {
        SleepUtils.sleep(Duration.ofMillis(2000));
        throw new IllegalArgumentException();
      }
    };

    CompletableFuture<Object> future = CompletableFuture.supplyAsync(CostedSupplier.of(supplier), executorService);
    CompletableFuture<Object> timeoutFuture = CompletableFutureUtils.orTimeout(future, Duration.ofMillis(1000));

    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    try {
      Object o = timeoutFuture.get();
    } catch (Throwable throwable) {
      atomicReference.set(throwable);
    }

    // --------------------------------------------------------------
    // 先超时，再内部异常。内部的任务会打印日志，扔出去的异常是TimeoutException。
    // --------------------------------------------------------------

    Assert.assertThrows(ExecutionException.class, () -> {
      throw atomicReference.get();
    });

    Assert.assertThrows(TimeoutException.class, () -> {
      throw atomicReference.get().getCause();
    });

    executorService.shutdown();
    executorService.awaitTermination(3000, TimeUnit.MILLISECONDS);
  }

}
