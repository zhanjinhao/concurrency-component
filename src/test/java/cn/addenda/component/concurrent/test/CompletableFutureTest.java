package cn.addenda.component.concurrent.test;

import cn.addenda.component.base.util.SleepUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CompletableFutureTest {

  @Test
  public void test() {

    ExecutorService executor = Executors.newFixedThreadPool(1);

    CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(new Supplier<String>() {
      @Override
      public String get() {
        System.out.println("start");
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        System.out.println("end");
        return null;
      }
    }, executor);

    SleepUtils.sleep(TimeUnit.SECONDS, 1);

    AtomicReference<Throwable> atomicReference = new AtomicReference<>();

    stringCompletableFuture.whenComplete(new BiConsumer<String, Throwable>() {
      @Override
      public void accept(String s, Throwable throwable) {
        atomicReference.set(throwable);
      }
    });

    executor.shutdownNow();
    SleepUtils.sleep(TimeUnit.SECONDS, 5);


    // ---------------------------------------------------------
    //  CompletableFuture内部扔出的异常都被包装为CompletionException
    // --------------------------------------------------------

    Assert.assertThrows(CompletionException.class, () -> {
      throw atomicReference.get();
    });

    Assert.assertThrows(RuntimeException.class, () -> {
      throw atomicReference.get().getCause();
    });

    Assert.assertThrows(InterruptedException.class, () -> {
      throw atomicReference.get().getCause().getCause();
    });

  }

}
