package org.neil.fluff.javacorners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Blocking calls have a big impact on shutting down threads cleanly. */
public class ExecutorShutdownTest
{
    private BlockingQueue<String> q;
    private ExecutorService       execSvc;
    
    @Before public void setUp() {
        // Each test needs a new queue...
        this.q = new LinkedBlockingQueue<>();
        
        // ...and a thread to take from the queue.
        execSvc = Executors.newSingleThreadExecutor();
    }
    
    @After public void ensureThreadsAreCleanedProperly() {
        execSvc.shutdownNow();
        try {
            boolean terminated = execSvc.awaitTermination(1000, TimeUnit.SECONDS);
            assertTrue(terminated);
        } catch (InterruptedException ignored) {
            // Intentionally empty
        }
    }
    
    @Test public void shutdownExecutorServiceContainingWaitingThread() throws InterruptedException {
        executeThreadThatTakesFromQueue();
        
        // Now request an orderly shutdown of the executor service.
        execSvc.shutdown();
        
        // In fact this will not end the thread that called take - it will sit there forever.
        boolean terminated = execSvc.awaitTermination(5, TimeUnit.SECONDS);
        
        assertFalse(terminated);
    }
    
    @Test public void forceShutdownExecutorServiceContainingWaitingThread() throws InterruptedException {
        executeThreadThatTakesFromQueue();
        
        // Now request a forced shutdown of the executor service.
        execSvc.shutdownNow();
        
        boolean terminated = execSvc.awaitTermination(5, TimeUnit.SECONDS);
        
        assertTrue(terminated);
    }
    
    private void executeThreadThatTakesFromQueue() {
        execSvc.execute(new Runnable() {
            @Override public void run() {
                try {
                    // This take call will never complete as there will never be anything in the queue to take.
                    q.take();
                } catch (InterruptedException e) {
                    System.out.println("exception in taking thread." + e);
                    Thread.interrupted();
                }
            }
        });
        
        // A little sleep to give the executor a chance to start its thread
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
            // Intentionally empty
        }
    }
}
