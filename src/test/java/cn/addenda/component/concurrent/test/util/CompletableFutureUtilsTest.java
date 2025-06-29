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

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            2,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            NamedThreadFactory.of("CompletableFutureUtilsTest", Executors.defaultThreadFactory()));

    log.info("start ");
    CompletableFuture<Void> f1 = CompletableFuture.runAsync(NamedRunnable.of("f1", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 30);
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f1, 10, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f1" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f2 = CompletableFuture.runAsync(NamedRunnable.of("f2", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 30);
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f2, 10, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f2" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f3 = CompletableFuture.runAsync(NamedRunnable.of("f3", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 30);
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f3, 10, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f3" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f4 = CompletableFuture.runAsync(NamedRunnable.of("f4", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 30);
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f4, 10, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f4" + throwable.toString());
              return null;
            });

    CompletableFuture<Void> f5 = CompletableFuture.runAsync(NamedRunnable.of("f5", name -> {
      SleepUtils.sleep(TimeUnit.SECONDS, 30);
      log.info(name + "执行完毕！");
    }), threadPoolExecutor);
    CompletableFutureUtils.orTimeout(f5, 10, TimeUnit.SECONDS).handle(
            (unused, throwable) -> {
              log.error("f5" + throwable.toString());
              return null;
            });

    new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          SleepUtils.sleep(TimeUnit.SECONDS, 5);
          log.info(threadPoolExecutor.toString());
        }
      }
    }).start();

    // 结论：超时后，正在执行中的任务会执行完成，排队的任务不会被线程执行
    SleepUtils.sleep(TimeUnit.SECONDS, 300);

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
