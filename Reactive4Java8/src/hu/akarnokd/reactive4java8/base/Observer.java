/*
* Copyright 2013 akarnokd.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package hu.akarnokd.reactive4java8.base;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base interface for observing a stream of values.
 * <p>reactive4java note: the interface has default empty
 * implementation for the error() and finish methods
 * to allow simple lambda expressions to register with
 * the {@link Observable} implementations.</p>
 * @author akarnokd, 2013.11.08
 * @param <T> the value type to be observed
 */
@FunctionalInterface
public interface Observer<T> extends BaseObserver {
    /**
     * Receive a value.
     * <p>Exceptions thrown by the method is forwarded to
     * the error() method.</p>
     * @param value the value received
     */
    void next(T value);
    /**
     * Creates an observer which calls the given consumer
     * when it receives an event, but forwards the error and
     * finish events to the given wrapped (base)observer.
     * @param <T> the value type of the observer
     * @param o the base observer to wrap
     * @param consumer the consumer of events
     * @return the new observer
     */
    static <T> Observer<T> wrap(BaseObserver o, Consumer<? super T> consumer) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                consumer.accept(value);
            }
            @Override
            public void error(Throwable t) {
                o.error(t);
            }
            @Override
            public void finish() {
                o.finish();
            }
        };
    }
    /**
     * Composes this observer with another by calling
     * the other's methods before this.
     * @param before
     * @return 
     */
    default Observer<T> composeWith(Observer<? super T> before) {
        Objects.requireNonNull(before);
        return create(
            (t) -> {
                before.next(t);
                next(t);
            },
            (e) -> {
                before.error(e);
                error(e);
            },
            () -> {
                before.finish();
                finish();
            }
        );
    }
    /**
     * Creates an observer by specifying its next and
     * error methods as the given consumer lambda functions.
     * @param <T>
     * @param next
     * @param error
     * @return 
     */
    public static <T> Observer<T> create(
            Consumer<? super T> next,
            Consumer<? super Throwable> error) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                next.accept(value);
            }
            @Override
            public void error(Throwable t) {
                error.accept(t);
            }
        };
    }
    /**
     * Creates an observer by specifying its next, error and
     * finish methods as the given consumer lambda functions.
     * @param <T>
     * @param next
     * @param error
     * @param finish
     * @return 
     */
    public static <T> Observer<T> create(
            Consumer<? super T> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(error);
        Objects.requireNonNull(finish);
        return new Observer<T>() {
            @Override
            public void next(T value) {
                next.accept(value);
            }
            @Override
            public void error(Throwable t) {
                error.accept(t);
            }
            @Override
            public void finish() {
                finish.run();
            }
        };
    }
    /**
     * Creates an observer by specifying its error and
     * finish methods as the given consumer lambda functions.
     * <p>The finish and error parameter order is swappend because the 
     * overloading would complain.</p>
     * @param <T>
     * @param finish
     * @param error
     * @return 
     */
    public static <T> Observer<T> create(
            Runnable finish,
            Consumer<? super Throwable> error
            ) {
        Objects.requireNonNull(error);
        Objects.requireNonNull(finish);
        return new Observer<T>() {
            @Override
            public void next(T value) { }

            @Override
            public void error(Throwable t) {
                error.accept(t);
            }
            
            @Override
            public void finish() {
                finish.run();
            }
        };
    }
    public static <T> Observer<T> create(Runnable finish) {
        Objects.requireNonNull(finish);
        return new Observer<T>() {
            @Override
            public void next(T value) { }

            @Override
            public void finish() {
                finish.run();
            }
            
        };
    }
    /**
     * Creates an observer which translates the
     * stream of U values via the function into a
     * stream of T values which is then forwarded
     * to this observer.
     * @param <U> the nw observed value type
     * @param function the function to apply to transform Us into Ts
     * @return the new observer of Us
     */
    default <U> Observer<U> compose(Function<? super U, ? extends T> function) {
        return wrap(this, (v) -> next(function.apply(v)));
    }
    /**
     * Converts this observer into a safe observer which
     * ensures the * {@code next* (error|finish)?}
     * event pattern on this observer.
     * @return the safe observer
     */
    default Observer<T> toSafeObserver() {
        if (this instanceof SafeObserver) {
            return this;
        }
        return new SafeObserver<>(this);
    }
    /**
     * Constructs a safe observer from the given lambda
     * expressions as the various event handlers where
     * the next() lambda is given the opportunity to cancel the
     * processing (error and finish events naturally stop the observer).
     * @param <T> the value type
     * @param sharedLock the shared lock instance
     * @param next
     * @param error
     * @param finish
     * @return the observer
     */
    public static <T> Observer<T> createSafe(Lock sharedLock,
            BiConsumer<? super T, ? super Registration> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        Objects.requireNonNull(sharedLock);
        Objects.requireNonNull(next);
        Objects.requireNonNull(error);
        Objects.requireNonNull(finish);
        
        return new Observer<T>() {
            private boolean done;
            protected void done() {
                this.done = done;
            }
            @Override
            public void next(T value) {
                if (!done) {
                    try {
                        next.accept(value, (Registration)this::done);
                    } catch (Throwable t) {
                        if (!done) {
                            done = true;
                            error.accept(t);
                        }
                    }
                }
            }

            @Override
            public void finish() {
                if (!done) {
                    done = true;
                    finish.run();
                }
            }

            @Override
            public void error(Throwable t) {
                if (!done) {
                    done = true;
                    error.accept(t);
                }
            }
        }.toThreadSafe(sharedLock);
    }
    /**
     * Constructs a safe observer from the given lambda
     * expressions as the various event handlers.
     * @param <T> the value type
     * @param sharedLock the shared lock instance
     * @param next
     * @param error
     * @param finish
     * @return the observer
     */
    public static <T> Observer<T> createSafe(Lock sharedLock,
            Consumer<? super T> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        Objects.requireNonNull(sharedLock);
        Objects.requireNonNull(next);
        Objects.requireNonNull(error);
        Objects.requireNonNull(finish);
        
        return new Observer<T>() {
            private boolean done;
            @Override
            public void next(T value) {
                if (!done) {
                    try {
                        next.accept(value);
                    } catch (Throwable t) {
                        done = true;
                        error.accept(t);
                    }
                }
            }

            @Override
            public void finish() {
                if (!done) {
                    done = true;
                    finish.run();
                }
            }

            @Override
            public void error(Throwable t) {
                if (!done) {
                    done = true;
                    error.accept(t);
                }
            }
        }.toThreadSafe(sharedLock);
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param error
     * @param finish
     * @return 
     */
    public static <T> Observer<T> createSafe(
            BiConsumer<? super T, ? super Registration> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        return createSafe(new ReentrantLock(), next, error, finish);
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param error
     * @param finish
     * @return 
     */
    public static <T> Observer<T> createSafe(
            Consumer<? super T> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        return createSafe(new ReentrantLock(), next, error, finish);
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param finish
     * @return 
     */
    public static <T> Observer<T> createSafe(
            Consumer<? super T> next,
            Runnable finish) {
        return createSafe(new ReentrantLock(), next, (t) -> { }, finish);
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param error
     * @return 
     */
    public static <T> Observer<T> createSafe(
            Consumer<? super T> next,
            Consumer<? super Throwable> error) {
        return createSafe(new ReentrantLock(), next, error, () -> { });
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param finish
     * @return 
     */
    public static <T> Observer<T> createSafe(
            BiConsumer<? super T, ? super Registration> next,
            Runnable finish) {
        return createSafe(new ReentrantLock(), next, (t) -> { }, finish);
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param next
     * @param error
     * @return 
     */
    public static <T> Observer<T> createSafe(
            BiConsumer<? super T, ? super Registration> next,
            Consumer<? super Throwable> error) {
        return createSafe(new ReentrantLock(), next, error, () -> { });
    }
    /**
     * Creates a safe observer with the given lambda
     * parameters as the observers event handler methods.
     * @param <T>
     * @param error
     * @param finish
     * @return 
     */
    public static <T> Observer<T> createSafe(
            Runnable finish,
            Consumer<? super Throwable> error) {
        return createSafe(new ReentrantLock(), (Object v) -> { } , error, finish);
    }
    /**
     * Returns an observer which feauteres exclusive locks between
     * its next(), error() and finish() method calls, but does
     * not enforce the event order semantics.
     * @return a thread safe observer
     */
    default Observer<T> toThreadSafe() {
        LockSync ls = new LockSync();
        return create(
            (t) -> ls.sync(() -> next(t)),
            (e) -> ls.sync(() -> error(e)),
            () -> ls.sync(() -> finish())
        );
    }
   /**
     * Returns an observer which feauteres exclusive locks between
     * its next(), error() and finish() method calls, but does
     * not enforce the event order semantics.
     * @param sharedLock the shared lock
     * @return 
     */
    default Observer<T> toThreadSafe(Lock sharedLock) {
        LockSync ls = new LockSync(sharedLock);
        return create(
            (t) -> ls.sync(() -> next(t)),
            (e) -> ls.sync(() -> error(e)),
            () -> ls.sync(() -> finish())
        );
    }
}
