package async4j;

import java.util.Queue;

public class LoadDistributor {

    private EnrichedThread[] pool = new EnrichedThread[0];

    /**
     * Removes as many runnables from the queue
     * as there is threads available in the distributor.
     * Then executes all.
     * @param runnables Total queue of all runnables.
     * @return how many runnables was able to be removed from the queue
     */
    public int distribute(Queue<EnrichedRunnable<?>> runnables)
    {

        int count = 0;
        for(EnrichedThread thread : pool) {
            if(runnables.isEmpty()) break;
            if(!verifyThreadState(thread)) continue;

            EnrichedRunnable<?> nextValidRunnable = getNextValidRunnable(runnables);

            if(verifyRunnable(nextValidRunnable)){
                thread.setNewTarget(nextValidRunnable);
                count++;
            }
        }
        return count;
    }
    private EnrichedRunnable<?> getNextValidRunnable(Queue<EnrichedRunnable<?>> runnables)
    {
        EnrichedRunnable<?> current = runnables.peek();
        while(!verifyRunnable(current) && !runnables.isEmpty())
        {
            current = runnables.poll();
        }
        return current;
    }

    /**
     * Determines wether or not the runnable should be run or is a valid runnable in the
     * first place.
     * @param runnable
     * @return
     */
    private boolean verifyRunnable(EnrichedRunnable<?> runnable)
    {
        return runnable != null
                && !runnable.isRunning.get()
                && !runnable.isDone.get();
    }
    private boolean verifyThreadState(EnrichedThread thread)
    {
        return thread.isAvailable();
    }

    /**
     * Shuts down the current thread pool and boots a new one
     * with the specified size. Duely note that this may
     * interfere with currently running processes.
     * @param size - pool size
     */
    public void setPoolSize(int size)
    {
        setPoolSize(size,500);
    }
    public void setPoolSize(int size, int maxDelay)
    {
        shutdown(maxDelay);
        pool = new EnrichedThread[size];
        for (int i = 0; i < pool.length; i++)
        {
            pool[i] = new EnrichedThread("Pool-Thread["+i+"]");
        }
    }

    /**
     * Shuts down all active threads in the pool
     * immediatly with no error mitigation
     */
    public void shutdown()
    {
        shutdown(0);
    }

    /**
     * Shuts down the current thread pool.
     * @param msecs - how many milliseconds each thread is allowed to take to shut down.
     *              (not total shutdown time)
     */
    public void shutdown(int msecs)
    {
        for(EnrichedThread thread : pool) {
            thread.shutdown(msecs);
        }
    }
}
