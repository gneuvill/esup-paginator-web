package epw.utils;

import fj.Effect;
import fj.Unit;
import fj.control.parallel.ParModule;
import fj.control.parallel.Strategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import static fj.control.parallel.ParModule.parModule;

public class ParallelModule {

  public static final ExecutorService execService = new ForkJoinPool(85, ForkJoinPool.defaultForkJoinWorkerThreadFactory, new Thread.UncaughtExceptionHandler() {
    public void uncaughtException(Thread thread, Throwable throwable) {
      throwable.printStackTrace();
      thread.interrupt();
    }
  }, true);

  public static final Strategy<Unit> unitStrategy =
      Strategy.<Unit>executorStrategy(execService).errorStrategy(new Effect<Error>() {
        public void e(Error error) {
          error.printStackTrace();
        }
      });

  //public static final Strategy<Unit> unitStrategy = completionStrategy(new ExecutorCompletionService<Unit>(execService));

  public static final ParModule parMod = parModule(unitStrategy);


}
