package org.neil.fluff.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neil.fluff.misc.ExceptionUtils.intercept;
import static org.neil.fluff.misc.Try.try_;

import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link Try}. Also usage examples.
 */
public class TryUnitTest
{
    /** This method always throws an unchecked exception. */
    private String alwaysFailUnchecked(String s)                  { throw new IllegalStateException("Intentionally failed"); }
    /** This method always throws a checked exception. */
    private String alwaysFailChecked(String s) throws IOException { throw new IOException("Intentionally failed"); }
    /** This method always returns a valid String. */
    private String alwaysSucceed(String s)                        { return s + "."; }

    @Test public void usingSuccessfulTry()
    {
        Try<String> tryOfString = try_(() -> alwaysSucceed("-"));
        assertTrue(tryOfString.isSuccess());
        assertEquals("-.", tryOfString.getOrThrow());
    }

    @Test public void usingFailedTry()
    {
        Try<String> tryOfString = try_(() -> alwaysFailUnchecked("-"));
        assertTrue(tryOfString.isFailure());
        intercept(IllegalStateException.class, () -> (String) tryOfString.getOrThrow());
    }

    @Test public void usingFailedTry_CheckedException()
    {
        Try<String> tryOfString = try_(() -> alwaysFailChecked("-"));
        assertTrue(tryOfString.isFailure());
        intercept(IOException.class, () -> (String) tryOfString.getOrThrow());
    }

    private Try<String> getSubStringAt(String s, int beginIndex, int endIndex)
    {
        return try_(() -> s.substring(beginIndex, endIndex));
    }

    private Try<Character> getCharAt(String s, int index) { return try_(() -> s.charAt(index)); }

    @Test public void mappingTryToTry()
    {
        // Create initial Trys.
        final Try<String> tryOfString       = try_(() -> alwaysSucceed("hello"));
        final Try<String> failedTryOfString = try_(() -> alwaysFailChecked("-"));

        // Map them to transformed Trys.
        //
        // 1. Mapping a successful Try successfully
        Try<Integer> stringLength1 = tryOfString.map((s) -> s.length());
        assertTrue(stringLength1.isSuccess());
        assertEquals("hello.", tryOfString.getOrThrow());
        assertEquals(new Integer(6), stringLength1.getOrThrow());

        // 2. Mapping a failed Try. This can never fail as the mapping is not actually performed.
        Try<Integer> stringLength2 = failedTryOfString.map((s) -> s.length());
        assertTrue(stringLength2.isFailure());
        intercept(IOException.class, () -> (String) failedTryOfString.getOrThrow());
        intercept(IOException.class, () -> (Integer) stringLength2.getOrThrow());

        // 3. Mapping a successful Try but failing during the mapping. This will actually throw an exception.
        // TODO Why is this cast needed?
        intercept(StringIndexOutOfBoundsException.class, () -> (Try<Character>) tryOfString.map(s -> s.charAt(100)));
    }

    @Test public void flatMappingTryToTry()
    {
        // Create initial Trys.
        final Try<String> tryOfString       = try_(() -> alwaysSucceed("hello"));
        final Try<String> failedTryOfString = try_(() -> alwaysFailChecked("-"));

        // FlatMap them to transformed Trys.
        //
        // 1. FlatMapping a successful Try successfully
        Try<Character> stringChar1 = tryOfString.flatMap((s) -> getCharAt(s, 1));
        assertTrue(stringChar1.isSuccess());
        assertEquals("hello.", tryOfString.getOrThrow());
        assertEquals(new Character('e'), stringChar1.getOrThrow());

        // 2. FlatMapping a failed Try.
        Try<Character> noChar = failedTryOfString.flatMap((s) -> getCharAt(s, 1));
        assertTrue(noChar.isFailure());
        intercept(IOException.class, () -> (String) failedTryOfString.getOrThrow());
        intercept(IOException.class, () -> (Character) noChar.getOrThrow());

        // 3. FlatMapping a successful Try but failing during the mapping. This will fail the Try but not throw an exception.
        Try<Character> failedFlatMap = tryOfString.flatMap(s -> getCharAt(s, 100));
        assertTrue(failedFlatMap.isFailure());
        intercept(StringIndexOutOfBoundsException.class, () -> (Character) failedFlatMap.getOrThrow());
    }

    /** An example of how to perform a sequence of operations that might fail - but don't in this case. */
    @Test public void chainingSuccessfulTry()
    {
        final String longString = "The_quick_brown_fox_jumped_over_the_lazy_dog.";

        // Note that each operation returns a Try<?> but that the flatMap operation ensures that we don't end up
        // with a Try<Try<Try<?>>>

        Try<String> subString = try_( () -> longString.substring(2, 20));               // "e quick..."
        Try<Character> tenthChar = subString.flatMap((s) -> getSubStringAt(s, 3, 18))   // "uick br..."
                                            .flatMap((s) -> getCharAt(s, 5));
        assertEquals(new Character('b'), tenthChar.getOrThrow());
    }

    /** An example of how to perform a sequence of operations that might fail - and do in this case. */
    @Test public void chainingFailingTry()
    {
        final String longString = "The_quick_brown_fox_jumped_over_the_lazy_dog.";

        Try<String> subString = try_( () -> longString.substring(2, 20));               // "e quick..."
        Try<Character> tenthChar = subString.thenTry((s) -> getSubStringAt(s, 3, 18))   // "uick br..."
                                            .thenTry((s) -> getCharAt(s, 100));         // This is out of bounds.
                                            // We could add more thenTry calls and they would not be called.
        assertTrue(tenthChar.isFailure());
        intercept(StringIndexOutOfBoundsException.class, () -> (Character) tenthChar.getOrThrow());

        // Or we could default to an alternative value
        char c = tenthChar.getOrElse('x');
        assertEquals('x', c);
    }
}