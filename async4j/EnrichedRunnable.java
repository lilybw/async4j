package async4j;

import async4j.iFunction;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnrichedRunnable<T> implements Runnable{

    public volatile AtomicBoolean isDone = new AtomicBoolean(false);
    public volatile AtomicBoolean isRunning = new AtomicBoolean(false);
    private T returnObject;
    public iFunction<T> runnable;

    public EnrichedRunnable(iFunction<T> runnable){
        Objects.requireNonNull(runnable);
        this.runnable = runnable;
    }

    @Override
    public void run() {
        isRunning.set(true);

        returnObject = runnable.execute();

        isDone.set(true);
        isRunning.set(false);
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
}