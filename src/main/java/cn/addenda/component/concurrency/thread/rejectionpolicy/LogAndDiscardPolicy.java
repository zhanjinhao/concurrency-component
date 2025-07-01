package cn.addenda.component.concurrency.thread.rejectionpolicy;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class LogAndDiscardPolicy extends ThreadPoolExecutor.DiscardPolicy {

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    log.error("ThreadPoolExecutor[{}] has been full, task[{}] submission failed!", executor, r);
    super.rejectedExecution(r, executor);
  }

}
