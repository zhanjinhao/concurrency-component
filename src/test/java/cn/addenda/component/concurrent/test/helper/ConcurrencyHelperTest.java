package cn.addenda.component.concurrent.test.helper;

import cn.addenda.component.base.collection.ArrayUtils;
import cn.addenda.component.base.lambda.wrapper.AttachmentRunnable;
import cn.addenda.component.base.lambda.wrapper.AttachmentSupplier;
import cn.addenda.component.base.util.SleepUtils;
import cn.addenda.component.concurrent.ConcurrencyException;
import cn.addenda.component.concurrent.helper.ConcurrencyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ConcurrencyHelperTest {

  @Test
  public void testAllGet_AllSuccess() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = IntStream.range(0, 3)
            .mapToObj(i -> AttachmentSupplier.of(10L, () -> "Result" + i))
            .collect(Collectors.toList());

    List<?> result = concurrencyHelper.allGet(suppliers, false, Duration.ofSeconds(10));

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.contains("Result0"));
    assertTrue(result.contains("Result1"));
    assertTrue(result.contains("Result2"));
  }

  @Test
  public void testAllGet_TaskThrowsException_FailFastFalse() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = ArrayUtils.asArrayList(
            AttachmentSupplier.of(10L, () -> "OK"),
            AttachmentSupplier.of(500L, () -> {
              SleepUtils.sleep(Duration.ofMillis(100));
              throw new RuntimeException("Boom");
            }),
            AttachmentSupplier.of(500L, () -> {
              SleepUtils.sleep(Duration.ofMillis(200));
              return "Another OK";
            })
    );

    long start = System.currentTimeMillis();
    List<?> result = concurrencyHelper.allGet(suppliers, false, Duration.ofSeconds(10));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last >= 200);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.get(0) instanceof String);
    assertTrue(result.get(1) instanceof RuntimeException);
    assertTrue(result.get(2) instanceof String);
  }

  @Test
  public void testAllGet_TaskThrowsException_FailFastTrue() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = ArrayUtils.asArrayList(
            AttachmentSupplier.of(10L, () -> "OK"),
            AttachmentSupplier.of(500L, () -> {
              SleepUtils.sleep(Duration.ofMillis(100));
              throw new RuntimeException("Boom");
            }),
            AttachmentSupplier.of(500L, () -> {
              SleepUtils.sleep(Duration.ofMillis(200));
              return "Another OK";
            })
    );

    long start = System.currentTimeMillis();
    List<?> result = concurrencyHelper.allGet(suppliers, true, Duration.ofSeconds(10));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last < 200);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.get(0) instanceof String);
    assertTrue(result.get(1) instanceof RuntimeException);
    assertTrue(result.get(2) == null);
  }

  @Test
  public void testAllGet_MainThreadTimeout() {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = ArrayUtils.asArrayList(
            AttachmentSupplier.of(1000L, () -> {
              SleepUtils.sleep(Duration.ofMillis(400));
              return "OK";
            }),

            AttachmentSupplier.of(1000L, () -> {
              SleepUtils.sleep(Duration.ofMillis(2000));
              return "Delayed Result";
            }),
            AttachmentSupplier.of(1000L, () -> {
              SleepUtils.sleep(Duration.ofMillis(400));
              return "OK2";
            })
    );

    long start = System.currentTimeMillis();
    List<?> result = concurrencyHelper.allGet(suppliers, false, Duration.ofMillis(500));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last < 800);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.get(0) instanceof String);
    assertTrue(ConcurrencyHelper.ifMainTimeout(result.get(1)));
    assertTrue(ConcurrencyHelper.ifMainTimeout(result.get(2)));
  }

  @Test
  public void testAllGet_TaskTimeout_FailFastFalse() throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = ArrayUtils.asArrayList(
            AttachmentSupplier.of(10L, () -> "OK1"),
            AttachmentSupplier.of(100L, () -> {
              SleepUtils.sleep(Duration.ofMillis(1000));
              return "OK2";
            }),
            AttachmentSupplier.of(200L, () -> {
              SleepUtils.sleep(Duration.ofMillis(2000));
              return "OK3";
            })
    );

    long start = System.currentTimeMillis();
    List<?> result = concurrencyHelper.allGet(suppliers, false, Duration.ofMillis(5000));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last >= 200L);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.get(0) instanceof String);
    assertTrue(result.get(1) instanceof TimeoutException);
    assertTrue(result.get(2) instanceof TimeoutException);
  }

  @Test
  public void testAllGet_TaskTimeout_FailFastTrue() throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentSupplier<Long, ?>> suppliers = ArrayUtils.asArrayList(
            AttachmentSupplier.of(10L, () -> "OK1"),
            AttachmentSupplier.of(10L, () -> {
              SleepUtils.sleep(Duration.ofMillis(2000));
              return "OK2";
            }),
            AttachmentSupplier.of(10L, () -> {
              SleepUtils.sleep(Duration.ofMillis(2000));
              return "OK3";
            })
    );

    long start = System.currentTimeMillis();
    List<?> result = concurrencyHelper.allGet(suppliers, true, Duration.ofMillis(5000));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last < 100L);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.get(0) instanceof String);
    assertTrue(result.get(1) instanceof TimeoutException);
    assertTrue(result.get(2) == null);
  }

  @Test
  public void testAllRun_AllSuccess() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    AtomicInteger counter = new AtomicInteger(0);
    List<AttachmentRunnable<Long>> runnableList = IntStream.range(0, 3)
            .mapToObj(i -> AttachmentRunnable.of(10L, () -> counter.incrementAndGet()))
            .collect(Collectors.toList());

    concurrencyHelper.allRun(runnableList, false, Duration.ofSeconds(10));

    assertEquals(3, counter.get());
  }

  @Test
  public void testAllRun_AllSuccess2() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    AtomicInteger counter = new AtomicInteger(0);
    List<Runnable> runnableList = IntStream.range(0, 3)
            .mapToObj(i -> new Runnable() {
              @Override
              public void run() {
                counter.incrementAndGet();
              }
            })
            .collect(Collectors.toList());

    concurrencyHelper.allRun(runnableList, Duration.ofMillis(10), false, Duration.ofSeconds(10));

    assertEquals(3, counter.get());
  }

  @Test
  public void testAllRunAndReportFirst_MainTimeout() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    AttachmentRunnable<Long> runnable = AttachmentRunnable.of(2000L, () -> {
      SleepUtils.sleep(Duration.ofMillis(1000));
    });

    long start = System.currentTimeMillis();
    assertThrows(ConcurrencyException.class, () ->
            concurrencyHelper.allRunAndReportFirst(ArrayUtils.asArrayList(runnable), Duration.ofMillis(500)));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last < 8000L);
  }

  @Test
  public void testAllRunAndReportFirst_TaskTimeout() {
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    AttachmentRunnable<Long> runnable = AttachmentRunnable.of(1000L, () -> {
      SleepUtils.sleep(Duration.ofMillis(2000));
    });

    long start = System.currentTimeMillis();
    assertThrows(ConcurrencyException.class, () ->
            concurrencyHelper.allRunAndReportFirst(ArrayUtils.asArrayList(runnable)));
    long end = System.currentTimeMillis();
    long last = end - start;
    Assert.assertTrue(last < 1200L);
  }

  @Test
  public void testAllRunAndReportFirst_TaskThrowsException() {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper((ThreadPoolExecutor) executorService);

    List<AttachmentRunnable<Long>> runnableList = ArrayUtils.asArrayList(
            AttachmentRunnable.of(10L, () -> {
            }),
            AttachmentRunnable.of(1000L, () -> {
              SleepUtils.sleep(Duration.ofMillis(500));
              throw new IllegalArgumentException("Boom1");
            }),
            AttachmentRunnable.of(1000L, () -> {
              SleepUtils.sleep(Duration.ofMillis(100));
              throw new NullPointerException("Boom2");
            })
    );

    long start = System.currentTimeMillis();
    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    try {
      concurrencyHelper.allRunAndReportFirst(runnableList);
    } catch (Throwable throwable) {
      atomicReference.set(throwable);
    }

    long end = System.currentTimeMillis();
    long last = end - start;

    Assert.assertTrue(last < 200);

    Assert.assertThrows(ConcurrencyException.class, () -> {
      throw atomicReference.get();
    });
    Assert.assertThrows(NullPointerException.class, () -> {
      throw atomicReference.get().getCause();
    });
  }

}
