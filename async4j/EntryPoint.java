package async4j;

import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntryPoint implements Runnable{

   private static final EntryPoint instance = new EntryPoint();
   private static volatile Queue<EnrichedRunnable<?>> queue = new ConcurrentLinkedQueue<>();
   private static final AtomicBoolean applicationIsRunning = new AtomicBoolean(true);
   private static final Thread manager = new Thread(instance);
   private static final LoadDistributor distributor = new LoadDistributor();

   private static long RETRY_DELAY_MSECS = 100L;

   static {
      Runtime.getRuntime().addShutdownHook(
           new Thread(EntryPoint::shutdown)
      );
   }

   /**
    * Offloads any code from the main thread to be distributed and executed
    * asynchronously.
    * @param runnable the code. () -> { your code here }
    * @param <T> the return type of said code, although it is implied automatically
    * @return The runnable enriched into an EnrichedRunnable
    */
   public static <T> EnrichedRunnable<T> async(iFunction<T> runnable)
   {
      EnrichedRunnable<T> enriched = new EnrichedRunnable<>(runnable);
      queue.add(enriched);
      synchronized ( instance ) {
         instance.notify();
      }
      return enriched;
   }

   public static <T> List<EnrichedRunnable<T>> async(List<iFunction<T>> list)
   {
      List<EnrichedRunnable<T>> toReturn = new ArrayList<>(list.size());
      for(iFunction<T> func : list) {
         toReturn.add(new EnrichedRunnable<>(func));
      }
      queue.addAll(toReturn);
      synchronized ( instance ) {
         instance.notify();
      }
      return toReturn;
   }

   /**
    * Awaits execution of given EnrichedRunnable.
    * @param runnable the runnable to wait for
    * @param <T> the type of value returned
    * @return null or T depending on the Runnable
    */
   public static <T> T await(EnrichedRunnable<T> runnable)
   {
      return await(runnable,0);
   }

   /**
    * Awaits execution of given EnrichedRunnable,
    * however, it will return if the time given runs out.
    * Do be aware that if the runnable hasn't been run by the thread
    * pool yet, the returned value will be null.
    * @param msecs the maximum time allowed.
    */
   public static  <T> T await(EnrichedRunnable<T> runnable, int msecs)
   {
      return runnable.get(msecs);
   }

   /**
    * Called only by the "Manager" thread. Which starts the entire loop
    * of waiting for new async calls, distributing these throughout the
    * threat pool and executing them.
    */
   @Override
   public void run()
   {
      long msecs = 0;

      synchronized (this) {
         while(applicationIsRunning.get()){
            try {
               this.wait(msecs, 0);

               distributor.distribute(queue);

               if (!queue.isEmpty()) {
                  msecs = RETRY_DELAY_MSECS;
               } else {
                  msecs = 0L;
               }

            } catch (InterruptedException e) {
               System.err.println("| FATAL | EntryPoint."
                       + Thread.currentThread().getStackTrace()[0].getLineNumber()
                       + " Manager was unexpectedly interrupted");
            }
         }
      }
   }

   public static void setRetryDelay(long msecs)
   {
      RETRY_DELAY_MSECS = msecs;
   }

   public static void initialize()
   {
      initialize(1);
   }

   /**
    * Starts the system. If this method is called multiple times,
    * it may affect currently concurrent processes.
    * @param poolSize - how many threads the system may utilize.
    */
   public static void initialize(int poolSize)
   {
      distributor.setPoolSize(poolSize);
      manager.start();
      System.out.println(">> ASYNC System Startup Successful");
   }

   /**
    * Immediately shuts down the system.
    * Do note that this function is already called automatically
    * using the Runtime.getShutDownHook() and shouldn't be used.
    */
   private static void shutdown()
   {
      applicationIsRunning.set(false);
      synchronized (instance) {
         instance.notify();
      }
      try {
         manager.join();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      distributor.shutdown();
      System.out.println(">> ASYNC || System Shutdown Successful");
   }
}
