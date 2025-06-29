package cn.addenda.component.concurrent.test;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LinkedBlockingQueueTest {

  @Test
  public void test1() throws Exception {
    BlockingQueue<Object> linkedBlockingQueue = new LinkedBlockingQueue<>();
    Object poll1 = linkedBlockingQueue.poll(10, TimeUnit.MILLISECONDS);
    Assert.assertNull(poll1);

    Object poll2 = linkedBlockingQueue.poll(-10, TimeUnit.MILLISECONDS);
    Assert.assertNull(poll2);
  }

}
