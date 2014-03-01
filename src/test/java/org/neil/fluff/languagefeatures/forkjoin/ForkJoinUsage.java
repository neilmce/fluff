package org.neil.fluff.languagefeatures.forkjoin;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * This class explores some of the usages of Java 7's ForkJoinPool.
 * <p/>
 * As you can see from the javadoc for ForkJoinPool, it's a special ExecutorService that
 * adds <em>work-stealing</em>. That is to say that all threads in this pool will actively try
 * to find work to do in order to minimise inactivity. This work could have been submitted to
 * the pool externally as a ForkJoinTask or it could have been created internally as a subtask
 * of an existing ForkJoinTask.
 * <p/>
 * The service is given ForkJoinTasks to execute. Although the API deals only with
 * ForkJoinTasks, it really expects to deal with two different types of such tasks:
 * <ol>
 * <li>small tasks (i.e. very fast)</li>
 * <li>large tasks (which probably need to be split up into smaller tasks before execution).</li>
 * </ol>
 * A key point is that the ForkJoin classes *expect* tasks to be split up into smaller
 * tasks, which will be new instances of ForkJoinTask and will be added into the same
 * thread pool for execution. (Think map-reduce).
 */
public class ForkJoinUsage {
    private static final Log LOG = LogFactory.getLog(ForkJoinUsage.class);
    // FIXME Why is logging not working for me?
    
    private ForkJoinPool forkJoinPool;
    
    @Before public void setUp() {
        // For most purposes, the common pool is sufficient.
        this.forkJoinPool = ForkJoinPool.commonPool();
    }
    
    /** A RecursiveTask is a ForkJoinTask that has an explicit return type. */
    @Test public void howToUseRecursiveTask() {
        // TODO
    }
    
    /** A RecursiveAction is a ForkJoinTask that does not have an explicit return type. */
    @Test public void howToUseRecursiveAction() {
        // Let's do a simple example. We want to sort a 'large' list, but we're
        // going to use divide & conquer to do it i.e. recursively sorting sublists
        // and then merging those sorted sublists.
        final String rawInput = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt" +
                                "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco" +
                                "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in" +
                                "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat" +
                                "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        final String cleanInput = rawInput.replaceAll("\\.", "").replaceAll(",", "").toLowerCase();
        
        List<String> input = new ArrayList<>();
        
        // Put individual words into the input.
        for (StringTokenizer t = new StringTokenizer(cleanInput); t.hasMoreTokens(); ) {
            input.add(t.nextToken());
        }
        
        // We can produce the expected result with a simple sort.
        List<String> expectedResult = new ArrayList<>(input);
        Collections.sort(expectedResult);
        
        // And we can produce the same result using a ForkJoinTask.
        final StringSorter task = new StringSorter(input);
        LOG.debug("About to add new task to the ForkJoinPool: " + task);
        
        // invoke() is a blocking call. We could alternatively call execute() or submit()
        // which would run the task asynchronously.
        // Calling join() on the task will then block for a result.
        forkJoinPool.invoke(task);
        forkJoinPool.submit(task);
        
        // At this scale of input the execution time will probably be the same. But for genuinely
        // large inputs and a multi-core machine, a ForkJoinPool should make better use of the cpu
        
        assertEquals(expectedResult, task.getResult());
    }
    
    /**
     * An example implementation of a divide and conquer algorithm using a Recursive Action.
     * This class can be used to sort lists of Strings according to their natural order.
     * It does this by only sorting sublists up to size {@link #SORT_MAX_SIZE}.
     * Any input larger than that will be broken up into smaller tasks sorting sections of the
     * input separately.
     * These various subtasks will then produce fragments of the result which
     * need to be merged together to get the final result.
     */
    private final class StringSorter extends RecursiveAction {
        private static final long serialVersionUID = 1L;
        
        /** Let's imagine that our StringSorter can't scale to input larger than 4 elements. */
        private static final int SORT_MAX_SIZE = 4;
        
        /** The input to this task. */
        private final List<String> input;
        /** The output of this task. */
        private List<String> result;
        
        public StringSorter(List<String> input) { this.input = input; }
        
        /** Gets the result of this task following {@link #compute() computation}. */
        public List<String> getResult() { return result; }
        
        @Override protected void compute() {
            // If the input is small enough, we'll delegate to the Java sort
            if (input.size() <= SORT_MAX_SIZE) {
                LOG.debug("sorting " + input.size() + " elements...");
                
                result = new ArrayList<>(input);
                Collections.sort(this.result);
            }
            // else we'll have to divide & conquer
            else {
                LOG.debug("splitting " + input.size() + " elements...");
                
                // So split the input in half
                final int indexToSplitAt = input.size() / 2;
                List<String> firstHalf   = input.subList(0, indexToSplitAt);
                List<String> secondHalf  = input.subList(indexToSplitAt, input.size());
                
                // And now create tasks to sort those two halfs separately
                StringSorter sortLHS = new StringSorter(firstHalf);
                StringSorter sortRHS = new StringSorter(secondHalf);
                
                // And invoke them...
                invokeAll(sortLHS, sortRHS);
                
                // .. and merge the results.
                this.result = merge(sortLHS.getResult(), sortRHS.getResult());
            }
        }
        
        /** Merge 2 lists of Strings maintaining sort order. */
        private List<String> merge(List<String> left, List<String> right) {
            List<String> result = new ArrayList<>();
            
            return merge(left, right, result);
        }
        
        /**
         * A recursive method to merge lists of sorted strings maintaining natural sort order.
         * 
         * @param left   the first input string.
         * @param right  the second input string
         * @param accumulator a list in which we accumulate the final result
         */
        private List<String> merge(List<String> left, List<String> right, List<String> accumulator) {
            if ( !left.isEmpty() && right.isEmpty()) {
                accumulator.addAll(left);
                // ... and we're done.
            }
            else if (left.isEmpty() && ! right.isEmpty()) {
                accumulator.addAll(right);
                // ... and we're done.
            }
            else if ( !left.isEmpty() && !right.isEmpty()) {
                // Now we need to take whichever list has the 'lowest' string.
                String leftHead = left.get(0);
                String rightHead = right.get(0);
                
                // We're either going to take from the left or from the right.
                boolean takeLeft = leftHead.compareTo(rightHead) < 0;
                
                if (takeLeft) {
                    accumulator.add(leftHead);
                    merge(left.subList(1, left.size()), right, accumulator);
                }
                else {
                    accumulator.add(rightHead);
                    merge(left, right.subList(1, right.size()), accumulator);
                }
            }
            
            return accumulator;
        }
    }
}
