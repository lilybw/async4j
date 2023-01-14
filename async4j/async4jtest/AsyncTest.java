package async4j.async4jtest;

import async4j.EnrichedRunnable;
import async4j.EntryPoint;
import async4j.iFunction;

import java.util.ArrayList;
import java.util.List;

import static async4j.EntryPoint.*;

public class AsyncTest {

    private static final double signifiancePercentage = 0.05D;

    public static void main(String[] args) {
        AsyncTest test = new AsyncTest();
        test.testArrayUseAsync();
        test.testSingleUseDelay();
        System.exit(69);
    }

    private void testSingleUseDelay()
    {
        System.out.println("|||_____Testing EntryPoint.delay_____|||");
        EntryPoint.initialize(4);
        final int expectedRunCompletionDelay = 1000;

        final List<Long> completionTimestamps = new ArrayList<>();
        List<iFunction<Boolean>> testRuns = new ArrayList<>();

        for(int i = 0; i < 10; i++) {
            final int num = i;
            testRuns.add(() -> {
                System.out.print(" " + num + ",");
                return completionTimestamps.add(System.currentTimeMillis());
            });
        }

        System.out.println();
        System.out.print("\t running test runs: ");
        final long testStart = System.currentTimeMillis();
        for(int i = 0; i < testRuns.size(); i++){
            delay(testRuns.get(i), i * expectedRunCompletionDelay);
        }

        try{
            synchronized (this) {
                wait(11_000);
            }
        }catch (InterruptedException e){
            System.err.println("Unexpected interruption during test: " + e.getMessage());
        }

        if(completionTimestamps.size() != 10){
            System.out.println(" !!! Error: Incomplete sampling");
        }

        boolean errorDetected = false;
        for(int i = 0; i < completionTimestamps.size(); i++){
            if((completionTimestamps.get(i) - testStart) % expectedRunCompletionDelay > expectedRunCompletionDelay * signifiancePercentage){
                System.out.println(" !!! Error: Completion delay outside " + (signifiancePercentage * 100) + "% significance margin");
                errorDetected = true;
                break;
            }
        }
        System.out.println();
        if(!errorDetected)
            System.out.print("\t Test completed successfully");
        System.out.println();

        EntryPoint.requestShutdown(500);
    }

    private void testArrayUseAsync()
    {
        final int poolSize = 4;
        EntryPoint.initialize(poolSize);

        System.out.println("|||_____Testing EntryPoint.async(array)_____|||");

        iFunction<String> testRun = () -> {
            int timeOutMs = 1000;
            long startTime = System.currentTimeMillis();
            while(System.currentTimeMillis() < startTime + timeOutMs){}
            return "testRun completed";
        };

        List<iFunction<String>> testRunList = new ArrayList<>();
        for(int i = 0; i < 10 * poolSize; i++){
            testRunList.add(testRun);
        }
        List<EnrichedRunnable<String>> runnables = async(testRunList);

        List<Double> timestampsOfBlockingCompletion = new ArrayList<>();
        System.out.print("\t completing test runs: ");
        for(int i = 0; i < runnables.size(); i++){
            await(runnables.get(i));
            timestampsOfBlockingCompletion.add((double) System.currentTimeMillis());
            System.out.print(" "+i+",");
        }

        //Expect staggered finish.
        boolean errorDetected = false;
        for(Double timestamp : timestampsOfBlockingCompletion){
            for(Double timestamp2 : timestampsOfBlockingCompletion){
                if(Math.abs((timestamp / timestamp2)) > timestamp * signifiancePercentage && !errorDetected){
                    System.out.println();
                    System.out.print(" !!! Error. Async array completion (blocking await) outside " + (signifiancePercentage * 100) + "% significance margin.");
                    errorDetected = true;
                    break;
                }
            }
        }

        System.out.println();
        if(errorDetected){
            System.out.print("\t timestamps: " + timestampsOfBlockingCompletion);
        }else{
            System.out.println("\t Async array completion within 5% significance margin");
        }
        System.out.println();

        EntryPoint.requestShutdown(500);
    }

}
