package async4j;

import java.util.concurrent.atomic.AtomicBoolean;

public class EnrichedThread implements Runnable{

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean shouldBeRunning = new AtomicBoolean(true);
    private final Thread thread;
    private EnrichedRunnable<?> currentRunnable;
    private String name;

    public EnrichedThread(String name)
    {
        thread = new Thread(this);
        thread.start();
        this.name = name;
    }
    public EnrichedThread()
    {
        this("EnrichedThread");
    }

    /**
     * Sets the target of the thread and notifies the object. Be aware that this does not take into account
     * wether the thread was already executing a runnable or not.
     * Check by calling the atomically based method isAvailable() first.
     * @param runnable the new target
     */
    public synchronized void setNewTarget(EnrichedRunnable<?> runnable)
    {
        running.set(true);
        currentRunnable = runnable;
        this.notify();
    }

    @Override
    public void run()
    {
        synchronized ( this ) {
            while (shouldBeRunning.get()) {
                try {
                    this.wait();
                    running.set(true);
                    if (currentRunnable != null) {
                        currentRunnable.run();
                    }
                    running.set(false);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * @return Wether or not it is safe to assign a new runnable to this thread.
     */
    public boolean isAvailable()
    {
        return !running.get();
    }

    /**
     * Joins and/or interrupts the thread within the time specified.
     * @param maxDelay how long this is allowed to take.
     */
    public void shutdown(int maxDelay)
    {
        shouldBeRunning.set(false);
        if(maxDelay <= 0){
            thread.interrupt();
        }

        try {
            thread.join(maxDelay);
        } catch (InterruptedException e) {
            thread.interrupt();
        }
    }
}
