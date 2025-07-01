package cn.addenda.component.concurrency.helper;

import cn.addenda.component.base.lambda.wrapper.AttachmentRunnable;
import cn.addenda.component.base.lambda.wrapper.AttachmentSupplier;
import cn.addenda.component.base.lambda.wrapper.CostedSupplier;
import cn.addenda.component.base.pojo.Ternary;
import cn.addenda.component.concurrency.ConcurrencyException;
import cn.addenda.component.concurrency.util.CompletableFutureUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConcurrencyHelper {

  private static final Object MAIN_TIMEOUT = new Object();

  private static final Object RUNNABLE_WRAPPER_RESULT = new Object();

  public static final Duration TEN_YEARS = Duration.ofDays(3650);

  private final ThreadPoolExecutor threadPoolExecutor;

  public ConcurrencyHelper(ThreadPoolExecutor threadPoolExecutor) {
    this.threadPoolExecutor = threadPoolExecutor;
  }

  public List<?> allGetAndReportFirst(List<AttachmentSupplier<Long, ?>> timeoutSupplierList) {
    return allGetAndReportFirst(timeoutSupplierList, TEN_YEARS);
  }

  public void allRunAndReportFirst(List<AttachmentRunnable<Long>> timeoutRunnableList) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = toTimeoutSupplierList(timeoutRunnableList);
    allGetAndReportFirst(timeoutSupplierList);
  }

  public List<?> allGetAndReportFirst(List<Supplier<?>> supplierList, Duration taskTimeout, Duration mainTimeout) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = supplierList.stream()
            .map(a -> AttachmentSupplier.of(taskTimeout.toMillis(), a)).collect(Collectors.toList());
    return allGetAndReportFirst(timeoutSupplierList, mainTimeout);
  }

  public void allRunAndReportFirst(List<Runnable> runnableList, Duration taskTimeout, Duration mainTimeout) {
    List<Supplier<?>> supplierList = toSupplierList(runnableList);
    allGetAndReportFirst(supplierList, taskTimeout, mainTimeout);
  }

  /**
   * @return 返回结果见 {@link ConcurrencyHelper#allGet(List, boolean, Duration)}。
   * <p>
   * throws：
   * <ul>
   *   <li> {@link RejectedExecutionException}：线程池满 </li>
   *   <li> {@link ConcurrencyException}：任务执行的第一个异常，或任务执行的第一个超时 </li>
   * </ul>
   */
  public List<?> allGetAndReportFirst(List<AttachmentSupplier<Long, ?>> timeoutSupplierList, Duration mainTimeout) {
    List<?> resultList = allGet(timeoutSupplierList, true, mainTimeout);
    for (Object result : resultList) {
      if (ifMainTimeout(result)) {
        throw new ConcurrencyException(new TimeoutException("main thread timeout"));
      } else if (result instanceof Throwable) {
        throw new ConcurrencyException((Throwable) result);
      }
    }
    return resultList;
  }

  public void allRunAndReportFirst(List<AttachmentRunnable<Long>> timeoutRunnableList, Duration mainTimeout) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = toTimeoutSupplierList(timeoutRunnableList);
    allGetAndReportFirst(timeoutSupplierList, mainTimeout);
  }

  public List<?> allGet(List<Supplier<?>> supplierList, Duration taskTimeout, boolean failFast) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = supplierList.stream()
            .map(a -> AttachmentSupplier.of(taskTimeout.toMillis(), a)).collect(Collectors.toList());
    return allGet(timeoutSupplierList, failFast, TEN_YEARS);
  }

  public void allRun(List<Runnable> runnableList, Duration taskTimeout, boolean failFast) {
    List<Supplier<?>> supplierList = toSupplierList(runnableList);
    allGet(supplierList, taskTimeout, failFast);
  }

  public List<?> allGet(List<Supplier<?>> supplierList, Duration taskTimeout, boolean failFast, Duration mainTimeout) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = supplierList.stream()
            .map(a -> AttachmentSupplier.of(taskTimeout.toMillis(), a)).collect(Collectors.toList());
    return allGet(timeoutSupplierList, failFast, mainTimeout);
  }

  public void allRun(List<Runnable> runnableList, Duration taskTimeout, boolean failFast, Duration mainTimeout) {
    List<Supplier<?>> supplierList = toSupplierList(runnableList);
    allGet(supplierList, taskTimeout, failFast, mainTimeout);
  }


  /**
   * 方法退出有三种可能。
   * <ul>
   *   <li>主线程执行时间超过mainTimeout。</li>
   *   <li>failFast为true，且任意一个任务异常</li>
   *   <li>所有任务完成</li>
   * </ul>
   *
   * @return 每一个Supplier的结果。
   * <ul>
   *   <li>如果任务正常完成，结果集合里对应索引位置的结果是值</li>
   *   <li>如果任务异常完成，结果集合里对应索引位置的结果是异常</li>
   *   <li>如果任务超时，结果集合里对应索引位置的结果是{@link TimeoutException}</li>
   *   <li>如果主线程超时，结果集合里对应索引位置的结果是{@link ConcurrencyHelper#MAIN_TIMEOUT}</li>
   *   <li>主线程超时或failFast为true时，未执行完成任务的结果为null</li>
   * </ul>
   */
  public List<?> allGet(List<AttachmentSupplier<Long, ?>> timeoutSupplierList, boolean failFast, Duration mainTimeout) {
    long start = System.currentTimeMillis();
    List<Object> resultList = createResultList(timeoutSupplierList.size());

    // --------
    //  提交任务
    // --------

    BlockingQueue<Ternary<Integer, Object, Throwable>> completionQueue = new LinkedBlockingQueue<>();
    List<CompletableFuture<?>> completableFutureList = new ArrayList<>();
    for (int i = 0; i < timeoutSupplierList.size(); i++) {
      if (ifMainTimeout(start, mainTimeout, completableFutureList, resultList) <= 0) {
        break;
      }

      AttachmentSupplier<Long, ?> timeoutSupplier = timeoutSupplierList.get(i);
      CostedSupplier<?> costedSupplier = CostedSupplier.of(timeoutSupplier, threadPoolExecutor);
      CompletableFuture<?> future2 = CompletableFutureUtils.orTimeout(
              // 这里可能扔出 RejectedExecutionException
              CompletableFuture.supplyAsync(costedSupplier, threadPoolExecutor),
              timeoutSupplier.getAttachment(), TimeUnit.MILLISECONDS);
      completableFutureList.add(future2);
      int finalI = i;
      future2.whenComplete((o, t) -> {
        if (t != null) {
          completionQueue.add(Ternary.of(finalI, null, t.getCause()));
        } else {
          completionQueue.add(Ternary.of(finalI, o, null));
        }
      });
    }

    // --------
    //  处理结果
    // --------

    try {
      int j = 0;
      do {
        long mainTimeoutMills;
        if ((mainTimeoutMills = ifMainTimeout(start, mainTimeout, completableFutureList, resultList)) <= 0) {
          break;
        }

        Ternary<Integer, Object, Throwable> poll = completionQueue.poll(mainTimeoutMills, TimeUnit.MILLISECONDS);
        if (poll == null) {
          // 当poll超时，会返回null
          continue;
        }
        Throwable throwable;
        if ((throwable = poll.getF3()) != null) {
          setResult(resultList, poll.getF1(), throwable);
          if (failFast) {
            cancel(completableFutureList);
            break;
          }
        } else {
          setResult(resultList, poll.getF1(), poll.getF2());
        }
        j++;
      } while (j < timeoutSupplierList.size());
    } catch (InterruptedException e) {
      throw new ConcurrencyException(e);
    }

    return resultList;
  }

  public void allRun(List<AttachmentRunnable<Long>> timeoutRunnableList, boolean failFast, Duration mainTimeout) {
    List<AttachmentSupplier<Long, ?>> timeoutSupplierList = toTimeoutSupplierList(timeoutRunnableList);
    allGet(timeoutSupplierList, failFast, mainTimeout);
  }

  private List<AttachmentSupplier<Long, ?>> toTimeoutSupplierList(List<AttachmentRunnable<Long>> timeoutRunnableList) {
    return timeoutRunnableList.stream()
            .map(new Function<AttachmentRunnable<Long>, AttachmentSupplier<Long, ?>>() {
              @Override
              public AttachmentSupplier<Long, ?> apply(AttachmentRunnable<Long> attachmentRunnable) {
                return AttachmentSupplier.of(attachmentRunnable.getAttachment(), new Supplier<Object>() {
                  @Override
                  public Object get() {
                    attachmentRunnable.run();
                    return RUNNABLE_WRAPPER_RESULT;
                  }

                  @Override
                  public String toString() {
                    return "ConcurrencyHelper.Supplier.Wrapper{" +
                            "runnable=" + attachmentRunnable.getRunnable() +
                            '}';
                  }
                });
              }
            }).collect(Collectors.toList());
  }

  private List<Supplier<?>> toSupplierList(List<Runnable> runnableList) {
    return runnableList.stream()
            .map(new Function<Runnable, Supplier<?>>() {
              @Override
              public Supplier<?> apply(Runnable runnable) {
                return new Supplier<Object>() {
                  @Override
                  public Object get() {
                    runnable.run();
                    return RUNNABLE_WRAPPER_RESULT;
                  }

                  @Override
                  public String toString() {
                    return "ConcurrencyHelper.Supplier.Wrapper{" +
                            "runnable=" + runnable +
                            '}';
                  }
                };
              }
            }).collect(Collectors.toList());
  }

  public static boolean ifMainTimeout(Object o) {
    return o == MAIN_TIMEOUT;
  }

  public static boolean ifRunnableWrapperResult(Object o) {
    return o == RUNNABLE_WRAPPER_RESULT;
  }

  private void cancel(List<CompletableFuture<?>> completableFutureList) {
    for (CompletableFuture<?> f : completableFutureList) {
      if (!f.isDone() && !f.isCancelled()) {
        f.cancel(true);
      }
    }
  }

  private void setResult(List<Object> resultList, int index, Object o) {
    synchronized (this) {
      if (resultList.get(index) != null) {
        return;
      }
      resultList.set(index, o);
    }
  }

  private void fillMainTimeout(List<Object> resultList) {
    for (int i = 0; i < resultList.size(); i++) {
      setResult(resultList, i, MAIN_TIMEOUT);
    }
  }

  private Long ifMainTimeout(Long start, Duration mainTimeout,
                             List<CompletableFuture<?>> completableFutureList, List<Object> resultList) {
    long end = System.currentTimeMillis();
    long mainTimeoutMills = mainTimeout.toMillis() - (end - start);

    if (mainTimeoutMills <= 0) {
      cancel(completableFutureList);
      fillMainTimeout(resultList);
    }

    return mainTimeoutMills;
  }

  private List<Object> createResultList(int size) {
    List<Object> resultList = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      resultList.add(null);
    }
    return resultList;
  }

}
