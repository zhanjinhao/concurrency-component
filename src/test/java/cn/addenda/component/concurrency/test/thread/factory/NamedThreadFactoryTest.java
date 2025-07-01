package cn.addenda.component.concurrency.test.thread.factory;

import cn.addenda.component.base.util.SleepUtils;
import cn.addenda.component.concurrency.thread.factory.NamedThreadFactory;
import cn.addenda.component.concurrency.thread.factory.UncaughtThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NamedThreadFactoryTest {

  @Test
  public void test1() throws Exception {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            UncaughtThreadFactory.of(NamedThreadFactory.of("NamedThreadFactoryTest", Executors.defaultThreadFactory())));
    for (int i = 0; i < 14; i++) {
      threadPoolExecutor.submit(() -> {
        SleepUtils.sleep(TimeUnit.SECONDS, 1);
        Assert.assertEquals("NamedThreadFactoryTest", Thread.currentThread().getName().split("-")[0]);
      });
    }

    threadPoolExecutor.awaitTermination(5, TimeUnit.SECONDS);
  }

}
