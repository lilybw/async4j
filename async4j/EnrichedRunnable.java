package async4j;

import async4j.iFunction;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnrichedRunnable<T> implements Runnable, Comparable<EnrichedRunnable<?>>{

    public volatile AtomicBoolean isDone = new AtomicBoolean(false);
    public volatile AtomicBoolean isRunning = new AtomicBoolean(false);
    public volatile long delayEnd = -1;
    public volatile int timeoutMS = -1;
    public volatile boolean repeating = false;
    private T returnObject;
    public iFunction<T> runnable;

    public EnrichedRunnable(iFunction<T> runnable){
        this(runnable, -1);
    }
    public EnrichedRunnable(iFunction<T> runnable, int timeoutMS){
        this(runnable,timeoutMS,false);
    }

    public EnrichedRunnable(iFunction<T> runnable, int timeoutMS, boolean repeating){
        Objects.requireNonNull(runnable);
        this.runnable = runnable;
        this.delayEnd = System.currentTimeMillis() + timeoutMS;
        this.timeoutMS = timeoutMS;
        this.repeating = repeating;
    }

    @Override
    public void run() {
        isRunning.set(true);

        returnObject = runnable.execute();

        if(repeating){
            updateDelayEnd();
            EntryPoint.repeatRunnable(this);
        }else{
            isRunning.set(false);
        }
        isDone.set(true);
    }

    public synchronized T get()
    {
        return get(0);
    }
    public synchronized T get(int msecs)
    {
        long endTime = System.currentTimeMillis() + msecs;
        if(!isDone.get()){
            if(msecs <= 0){
                while(!isDone.get()){
                    System.out.print("");
                }
            }else {
                while (System.currentTimeMillis() < endTime && !isDone.get()) {
                    System.out.print("");
                }
            }
        }
        return returnObject;
    }

    private void updateDelayEnd()
    {
        this.delayEnd = timeoutMS + System.currentTimeMillis();
    }

    @Override
    public int compareTo(EnrichedRunnable<?> o) {
        return Long.compare(this.delayEnd,o.delayEnd);
    }
}