package org.neil.fluff.misc;

import java.util.function.Function;

/**
 * A {@code Try} object represents the result of a completed computation that may or may not have succeeded. It is an
 * alternative technique to throwing &amp; catching exceptions which can be useful in some circumstances.
 * It is a monad and therefore supports the monadic composition of potentially failing operations whilst relieving
 * the coder from responsibility for dealing with exceptions during the chain of operations.
 * <p/>
 * A {@link #isSuccess successful} {@code Try} object yields its value through {@link #getOrThrow()}
 * whereas a {@link #isFailure() failed} {@code Try} object instead contains the exception object thrown
 * by the computation, which will be thrown by {@link #getOrThrow()}.
 *<p/>
 * Example usage:
 * Imagine we want to apply a chain of transformation operations to this String and that any of them can fail.
 * However we don't want to worry about individual failures, we simply want to run the chain to completion
 * and deal with any exceptions (failures) at the end.
 * <pre>
 * final String longString = "The_quick_brown_fox_jumped_over_the_lazy_dog.";
 *
 * Try<String> subString = try_( () -> longString.substring(2, 20));               // "e quick..."
 * Try<Character> tenthChar = subString.thenTry((s) -> getSubStringAt(s, 3, 18))   // "uick br..."
 *                                     .thenTry((s) -> getCharAt(s, 100));         // This is out of bounds.
 * // We could add more thenTry calls and they would be short-circuited.
 *
 * // The Try has failed...
 * assertTrue(tenthChar.isFailure());
 * // ...and contains the exception representing that failure.
 * intercept(StringIndexOutOfBoundsException.class, () -> (Character) tenthChar.getOrThrow());
 *
 * // Or we could default to an alternative value for failed Trys.
 * char c = tenthChar.getOrElse('x');
 *
 * assertEquals('x', c);
 * </pre>
 *
 * @param <R> the type of the successful value.
 */
public abstract class Try<R>
{
    /** Executes the supplied block of code returning the result wrapped in a {@code Try}. */
    public static <R, T extends Exception> Try<R> try_(ThrowingSupplier<R, T> s)
    {
        try                 { return Try.succeeded(s.get()); }
        catch (Exception e) { return Try.failed(e); }
    }

    private Try() { /* Intentionally empty. */ }

    /** Was this {@code Try} completed successfully? */
    public abstract boolean isSuccess();

    /** Was this {@code Try} completed with an exception? */
    public boolean isFailure() { return !isSuccess(); }

    /** This method creates a successfully completed {@code Try} with the specified value. */
    public static <R> Try<R> succeeded (R value)     { return new Success(value); }

    /** This method creates a {@code Try} unsuccessfully completed with the specified exception object. */
    public static <R> Try<R> failed    (Throwable t) { return new Failure(t); }

    /** Either returns the successfully completed value of this {@code Try} or throws
     * the exception contained in a failed {@code Try}. */
    public abstract <T extends Throwable> R getOrThrow() throws T;

    /**
     * If this {@code Try} is {@link #isSuccess() successful} returns the value within it.
     * If it is a {@link #isFailure() failed} {@code Try} returns the provided alternative value.
     *
     * @param altValue the alternative value to return if this {@code Try} is failed.
     * @return the value held within this {@code Try} if successful, else the altValue.
     */
    public abstract R getOrElse(R altValue);

    /**
     * Maps this {@code Try} to a new value if it is {@link #isSuccess() successful} by applying the provided function
     * to the current value.
     *
     * @param mapper a function which maps the current value to a new value.
     * @param <R2> the type of the new value.
     * @param <EX> the type of exception which may be thrown by the mapper function.
     * @return the mapped value if this {@code Try} is successful, else a {@link #isFailure() failed} {@code Try} object
     *         which is equivalent to this.
     * @throws EX if the mapper function throws an exception, it will be thrown from this method.
     * @see #map(ThrowingFunction) which is exactly equivalent.
     */
    public <R2, EX extends Exception> Try<R2> then(ThrowingFunction<R, R2, EX> mapper) throws EX { return map(mapper); }

    /**
     * Maps (technically 'flatMaps') this {@code Try} to a new value if it is {@link #isSuccess() successful} by applying
     * the provided function to the current value.
     * Note that unlike {@link #then(ThrowingFunction)} if the provided {@code mapper} function throws an exception
     * then it will not be thrown from this method and will be caught resulting in a {@link #isFailure() failed} {@code Try}.
     *
     * @param mapper a function which maps the current value to a new value.
     * @param <R2> the type of the new value.
     * @return the mapped value if this {@code Try} is successful, else a {@link #isFailure() failed} {@code Try} object
     *         which is equivalent to this.
     * @see #flatMap(Function) which is exactly equivalent.
     */
    public <R2> Try<R2> thenTry(Function<R, Try<R2>> mapper) { return flatMap(mapper); }

    /** @see #then(ThrowingFunction) */
    public abstract <R2, EX extends Exception> Try<R2> map(ThrowingFunction<R, R2, EX> mapper) throws EX;

    /** @see #thenTry(Function) */
    public abstract <R2> Try<R2> flatMap(Function<R, Try<R2>> flatMapper);

    /** This {@code Try} contains a successfully resolved value. */
    private static class Success<R> extends Try<R>
    {
        private final R value;
        private Success(R value) { this.value = value; }

        @Override public boolean isSuccess() { return true; }

        @Override public R getOrThrow() { return value; }

        @Override public R getOrElse(R altValue) { return value; }

        @Override public <R2, EX extends Exception> Try<R2> map(ThrowingFunction<R, R2, EX> mapper) throws EX
        {
            return succeeded(mapper.apply(value));
        }

        @Override public <R2> Try<R2> flatMap(Function<R, Try<R2>> flatMapper)
        {
            return flatMapper.apply(value);
        }
    }

    /** This {@code Try} has failed and contains the exception object representing that failure. */
    private static class Failure<R> extends Try<R>
    {
        private final Throwable t;
        private Failure(Throwable t) { this.t = t; }

        @Override public boolean isSuccess() { return false; }

        @Override public <T extends Throwable> R getOrThrow() throws T { throw (T)t; }

        @Override public R getOrElse(R altValue) { return altValue; }

        @Override public <R2, EX extends Exception> Try<R2> map(ThrowingFunction<R, R2, EX> mapper) throws EX
        { return failed(t); }

        @Override public <R2> Try<R2> flatMap(Function<R, Try<R2>> flatMapper)
        { return failed(t); }
    }

    @FunctionalInterface public interface ThrowingSupplier<RES, EX extends Exception>
    {
        RES get() throws EX;
    }

    @FunctionalInterface public interface ThrowingFunction<T, RES, EX extends Exception>
    {
        RES apply(T t) throws EX;
    }
}