package cn.addenda.component.concurrency.test.allocator.performance;

import cn.addenda.component.concurrency.allocator.lock.LockAllocator;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * @author addenda
 * @since 2023/6/3 12:36
 */
public abstract class Test_Base_Allocator {

  LockAllocator<? extends Lock> lockAllocator;

  public Test_Base_Allocator(LockAllocator<? extends Lock> lockAllocator) {
    this.lockAllocator = lockAllocator;
  }

  public void baseTest() {
    List<Long> list = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      long start = System.currentTimeMillis();
      doTest();
      long end = System.currentTimeMillis();
      list.add(end - start);
    }
    list.sort(Comparator.naturalOrder());

    long sum = 0;
    for (int i = 5; i < 95; i++) {
      sum = list.get(i) + sum;
    }
    System.out.println("avg : " + sum / 90 + " ms");
  }

  private int THREAD_COUNT = 100;
  private int DATA_SIZE = 10;

  public void doTest() {
    long start = System.currentTimeMillis();
    int[] intArray = createIntArray(DATA_SIZE);

    List<Thread> threadList1 = multiThreadRun(f -> intArray[f]++);
    List<Thread> threadList2 = multiThreadRun(f -> intArray[f]--);

    for (int i = 0; i < 100; i++) {
      threadList1.get(i).start();
      threadList2.get(i).start();
    }

    join(threadList1);
    join(threadList2);

    assertResult(intArray);

    long end = System.currentTimeMillis();

//        System.out.printf(
//            "cost %s ms \t\t, result: %s"
//            , (end - start)
//            , Arrays.stream(intArray).mapToObj(String::valueOf).collect(Collectors.joining()));
//        System.out.println();

  }

  private void assertResult(int[] intArray) {
    for (int i = 0; i < DATA_SIZE; i++) {
      if (intArray[i] != 0) {
        System.out.println(intArray[i]);
//        throw new RuntimeException();
      }
    }
  }

  private List<Thread> multiThreadRun(Consumer<Integer> consumer) {
    List<Thread> threadList = new ArrayList<>();
    for (int i = 0; i < THREAD_COUNT; i++) {
      int finalI = i % 10;
      threadList.add(new Thread(() -> {
        for (int j = 0; j < 1000; j++) {
          Lock test = lockAllocator.allocate("test" + finalI);
          test.lock();
          try {
            consumer.accept(finalI);
          } finally {
            test.unlock();
            lockAllocator.release("test" + finalI);
          }
        }
      }));
    }
    return threadList;
  }

  private int[] createIntArray(int size) {
    int[] values = new int[size];
    for (int i = 0; i < size; i++) {
      values[i] = 0;
    }
    return values;
  }

  @SneakyThrows
  private void join(List<Thread> threadList) {
    for (Thread thread : threadList) {
      thread.join();
    }
  }

}
