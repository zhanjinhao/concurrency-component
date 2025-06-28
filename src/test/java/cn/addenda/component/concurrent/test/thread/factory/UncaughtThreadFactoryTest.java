package cn.addenda.component.concurrent.test.thread.factory;

import cn.addenda.component.concurrent.thread.factory.NamedThreadFactory;
import cn.addenda.component.concurrent.thread.factory.UncaughtThreadFactory;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UncaughtThreadFactoryTest {

  @Test
  public void test1() throws Exception {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            UncaughtThreadFactory.of(NamedThreadFactory.of(Executors.defaultThreadFactory())));
    for (int i = 0; i < 14; i++) {
      threadPoolExecutor.execute(() -> {
        throw new RuntimeException("for purpose!");
      });
    }

    threadPoolExecutor.awaitTermination(5, TimeUnit.SECONDS);
  }

}
