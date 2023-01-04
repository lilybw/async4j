# async4j
Thread pool-based automatic concurrent execution for java.  
Introduces an entry point with static methods async() which can take any code, and functions, and distribute its execution on to a configurable thread pool.

NB: This is in beta. I've yet to impliment repeated and delayed concurrent execution.

# How To Set This Up
0. Open any of your java projects. 
1. Copy and paste this package into your SRC. 
2. Statically import the static async functions (or do it normally) from EntryPoint.java where you want to use it.
3. Call EntryPoint.initialize() in your main method providing a pool size. 

# How To Use This
0. Encapsulate any code in Entrypoint.async( {your code here} )
1. Use Entrypoint.await(some concurrently running code) to have your main thread wait for the result and then return this.
2. Use EnrichedRunnable.get(0) if you're sure enough its done computing (not recommended, can return null)
Your encapsulated code will now automatically be distributed throughout the pool and executed as fast as possible.

# Got Issues With Dependencies?
This package was developed using the Java.util.concurrency.ConcurrentBlockingQueue. Any implimentation of a concurrent blocking queue will do in its stead. 
Everything else requires the java standard library only.
