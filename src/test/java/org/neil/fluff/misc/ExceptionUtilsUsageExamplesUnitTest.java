package org.neil.fluff.misc;

import static org.junit.Assert.assertEquals;
import static org.neil.fluff.misc.ExceptionUtils.MissingThrowableException;
import static org.neil.fluff.misc.ExceptionUtils.UnexpectedThrowableException;
import static org.neil.fluff.misc.ExceptionUtils.intercept;

import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests showing usage of {@link ExceptionUtils}.
 */
public class ExceptionUtilsUsageExamplesUnitTest
{
    private String goodMethod() { return "hello"; }

    private String badMethod1()  { throw new RuntimeException("Bad method"); }

    private String badMethod2() { throw new UnsupportedOperationException("Bad method", new RuntimeException("root cause")); }

    @Test public void swallowExpectedExceptions()
    {
        // Calling a local method. (An expression lambda)
        intercept(RuntimeException.class, () -> badMethod1());

        // Executing a block of code. (Requires return statement)
        intercept(RuntimeException.class, () ->
        {
            for (int i = 0; i < 10; i++) {
                goodMethod();
            }
            // Also works for subtypes of expected exception.
            badMethod2();
            return null;
        });
    }

    @Test public void examineTheExpectedException()
    {
        UnsupportedOperationException e = intercept(UnsupportedOperationException.class, () -> badMethod2());
        assertEquals(RuntimeException.class, e.getCause().getClass());
    }

    @Test(expected=MissingThrowableException.class)
    public void expectedExceptionNotThrown()
    {
        intercept(IOException.class, () ->
        {
            // Do nothing
            return null;
        });
    }

    @Test(expected=UnexpectedThrowableException.class)
    public void unexpectedExceptionThrown()
    {
        intercept(IOException.class, () ->
        {
            throw new UnsupportedOperationException();
        });
    }

    private void onlySideEffectsHere() { throw new IllegalStateException(); }

    private void onlySideEffectsHere(String s) { throw new IllegalStateException(); }

    // If you use lambdas that return void, then they cannot be lambda expressions. They must be blocks.
    @Test public void usingVoidLambdas()
    {
        intercept(IllegalStateException.class, () -> {
            onlySideEffectsHere();
            return null;
        });

        intercept(IllegalStateException.class, () -> {
            onlySideEffectsHere("hello");
            return null;
        });
    }
}