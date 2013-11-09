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
     * evnet pattern on this observer.
     * @return the safe observer
     */
    default Observer<T> toSafeObserver() {
        if (this instanceof SafeObserver) {
            return this;
        }
        return new SafeObserver(this);
    }
    /**
     * Observer wrapper that ensures the
     * {@code next* (error|finish)?} event pattern on its wrapped observer.
     * @param <T> the value type
     */
    public static final class SafeObserver<T> implements Observer<T> {
        private final Observer<T> wrapped;
        private final Lock lock;
        private boolean done;
        /**
         * Constructor, wraps the observer.
         * @param observer the observer to wrap
         */
        public SafeObserver(Observer<T> observer) {
            this.wrapped = Objects.requireNonNull(observer);
            lock = new ReentrantLock();
        }
        protected void sync(Runnable run) {
            lock.lock();
            try {
                run.run();
            } finally {
                lock.unlock();
            }
        }
        @Override
        public void next(T value) {
            sync(() -> {
                if (!done) {
                    try {
                        wrapped.next(value);
                    } catch (Throwable t) {
                        error(t);
                    }
                }
            });
        }

        @Override
        public void error(Throwable t) {
            sync(() -> {
                if (!done) {
                    done = true;
                    wrapped.error(t);
                }
            });
        }

        @Override
        public void finish() {
            sync(() -> {
                if (!done) {
                done = true;
                    wrapped.finish();
                }
            });
        }
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
            private final Lock lock = sharedLock;
            private boolean done;
            protected void sync(Runnable run) {
                lock.lock();
                try {
                    try {
                        if (!done) {
                            run.run();
                        }
                    } catch (Throwable t) {
                        done();
                        error.accept(t);
                    }
                } finally {
                    lock.unlock();
                }
            }
            protected void done() {
                this.done = true;
            }
            @Override
            public void next(T value) {
                sync(() -> {
                   next.accept(value, (Registration)this::done); 
                });
            }

            @Override
            public void finish() {
                sync(() -> {
                    done();
                    finish.run();
                });
            }

            @Override
            public void error(Throwable t) {
                sync(() -> {
                    done();
                    error.accept(t);
                });
            }
        };
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
            private final Lock lock = sharedLock;
            private boolean done;
            protected void sync(Runnable run) {
                lock.lock();
                try {
                    try {
                        if (!done) {
                            run.run();
                        }
                    } catch (Throwable t) {
                        done();
                        error.accept(t);
                    }
                } finally {
                    lock.unlock();
                }
            }
            protected void done() {
                this.done = true;
            }
            @Override
            public void next(T value) {
                sync(() -> {
                   next.accept(value); 
                });
            }

            @Override
            public void finish() {
                sync(() -> {
                    done();
                    finish.run();
                });
            }

            @Override
            public void error(Throwable t) {
                sync(() -> {
                    done();
                    error.accept(t);
                });
            }
        };
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
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
    public static <T> Observer createSafe(
            Runnable finish,
            Consumer<? super Throwable> error) {
        return createSafe(new ReentrantLock(), (Object v, Object u) -> { } , error, finish);
    }
}
