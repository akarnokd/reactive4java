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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Defines provider for push-based value streaming.
 * <p>reactive4java notes:
 * <ul>
 * <li>the interface defines the most
 * common operations which were previously in the Reactive
 * utility class.</li>
 * <li>The register() method now returns a {@link Registration}
 * instance instead of a {@link java.io.Closeable} since most cancel
 * operations don't throw IOException, but the previous library was
 * limited to Java 6 constructs.</li>
 * </ul></p>
 * @author akarnokd, 2013.11.08
 * @param <T> the type of the vales to be streamed
 */
@FunctionalInterface
public interface Observable<T> {
    /**
     * Registers an observer with this observable.
     * @param observer the observer to register
     * @return the registration which can be used to deregister
     * the observer.
     */
    Registration register(Observer<? super T> observer);
    
    // most common operators
    /**
     * Apply the mapping function to the observable to produce
     * new value for each incoming value.
     * @param <U> the output value type
     * @param function the function to apply
     * @return the new observable of Us
     */
    default <U> Observable<U> select(Function<? super T, U> function) {
        return (observer) -> 
           register(observer.compose(function))
        ;
    }
    /**
     * Filters the observed value with the given predicate
     * and pushes values which test true with it.
     * @param predicate the predicate to use
     * @return the new observable
     */
    default Observable<T> where(Predicate<? super T> predicate) {
        return (observer) ->
            register(Observer.wrap(observer, 
                    (v) -> { if (predicate.test(v)) { observer.next(v); } }))
           
        ; 
    }
    /**
     * Returns an observable sequence of the given values
     * which are immediately pushed to the observers.
     * @param <T> the value type
     * @param values the values
     * @return the created observable
     */
    @SafeVarargs
    public static <T> Observable<T> from(T... values) {
        return (observer) -> {
            for (T t : values) {
                observer.next(t);
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Returns an observable sequence that iterates over
     * the source sequence pushes each value immediately
     * to the observers.
     * @param <T> the value type
     * @param src the source value sequence
     * @return the created observable
     */
    public static <T> Observable<T> from(Iterable<? extends T> src) {
        return (observer) -> {
            for (T t : src) {
                observer.next(t);
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Creates an observable sequence from the given stream.
     * <p>Warning! Streams are not thread safe and can be
     * consumed only once so avoid do not register
     * to the underlying stream twice!</p>
     * FIXME this method is generally dangerous
     * @param <T>
     * @param stream
     * @return 
     */
    public static <T> Observable<T> from(Stream<? extends T> stream) {
        Object sync = new Object();
        return (observer) -> {
            synchronized (sync) {
                stream.forEach(observer::next);
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Runs the given action before pushing the 
     * incoming value to the observer.
     * @param action the action to invoke
     * @return the new observable
     */
    default Observable<T> run(Runnable action) {
        return (observer) -> 
            register(Observer.wrap(observer, 
                    (v) -> { action.run(); observer.next(v); }))
        ;
    }
    /**
     * Synchronously consumes the observable sequence and calls
     * the given consumer with each value observed.
     * If the current thread is interrupted while this runs,
     * the observer is deregistered and the interrupted exception consumed.
     * @param consumer the consumer to use
     */
    default void forEach(Consumer<? super T> consumer) {
        CountDownLatch latch = new CountDownLatch(1);
        try (Registration reg = register(Observer.create(
                consumer::accept, 
                (t) -> { latch.countDown(); },
                () -> { latch.countDown(); }
            ))) {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // otherwise ignored
        }
    }
    default void forEach(IndexedConsumer<? super T> consumer) {
        IntRef count = new IntRef();
        forEach(v -> consumer.accept(count.value++, v));
    }
    default <U> Observable<U> select(IndexedFunction<? super T, ? extends U> function) {
        return (observer) -> {
            IntRef count = new IntRef();
            return register(observer.compose(v -> function.apply(count.value++, v)));
        };
    }
    default Observable<T> where(IndexedPredicate<? super T> predicate) {
        return (observer) -> {
            IntRef count = new IntRef();
            return register(Observer.wrap(observer,
                    (v) -> { if (predicate.test(count.value++, v)) { observer.next(v); } }));
        };
    }
    /**
     * Retunrs an observable sequence which contains
     * distinct values according to Objects.equals().
     * @return the observable with distinct values
     */
    default Observable<T> distinct() {
        return (observer) -> {
            Set<T> set = new HashSet<>();
            return where(set::add).register(observer);
        };
    }
    /**
     * Returns an observable sequence which pushes
     * subsequent values only if they are not
     * equal according to the Objects.equals() method.
     * <p>For example, a sequence of A, A, B, B, A,  D
     * will be returned as A, B, A, D.</p>
     * @return the new observable
     */
    default Observable<T> distinctUntilChanged() {
        return (observer) -> {
            Ref<T> ref = new Ref<>();
            BoolRef first = new BoolRef();
            first.value = true;
            return where(v -> { 
                if (first.value || !Objects.equals(ref.value, v)) {
                    ref.value = v;
                    first.value = false;
                    return true;
                }
                ref.value = v;
                return false;
            }).register(observer);
        };
    }
    public static <T> Observable<T> empty() {
        return (observer) -> {
            observer.finish();
            return Registration.EMPTY;
        };
    }
    public static <T> Observable<T> never() {
        return (observer) -> {
            Ref<Object> ref = new Ref<>();
            ref.value = observer;
            return () -> {
                ref.value = null;
            };
        };
    }
    /**
     * Takes the first {@code count} elements from
     * this observable sequence and finishes.
     * @param count the number of elements to take
     * @return the new observable
     */
    default Observable<T> take(int count) {
        if (count == 0) {
            return empty();
        }
        return (observer) -> {
            IntRef counter = new IntRef();
            counter.value = count;
            // FIXME close uplink registration ???
            return register(Observer.create(
                (v) -> { 
                    if (counter.value-- > 0) {
                        observer.next(v);
                        if (counter.value == 0) {
                            observer.finish();
                        }
                    }
                },
                (t) -> { observer.error(t); },
                () -> { observer.finish(); }
            ));
        };
    }
    /**
     * Returns an observable which makes sure the all events
     * are observed on the given scheduler.
     * @param scheduler the scheduler to use
     * @return the observable
     */
    default Observable<T> observeOn(Scheduler scheduler) {
        return (observer) -> {
            SingleLaneScheduling sls = new SingleLaneScheduling(scheduler);
            
            CompositeRegistration creg = new CompositeRegistration();
            creg.add(sls);
            creg.add(register(Observer.createSafe(
                  (T v) -> { // XXX Inference loop without the T???
                      sls.schedule(() -> {
                          try {
                              observer.next(v);
                          } catch (Throwable t) {
                              observer.error(t);
                          }
                      });
                  },
                  (t) -> {
                      sls.schedule(() -> {
                          observer.error(t);
                      });
                  },
                  () -> {
                      sls.schedule(() -> {
                          observer.finish();
                      });
                  }
            
            )));
            
            return creg;
        };
    }
    /**
     * Returns an observable which makes sure the
     * registrations and unregistrations happen on the given
     * scheduler (but the events are not observed there).
     * @param scheduler the scheduler to use
     * @return the observable
     */
    default Observable<T> registerOn(Scheduler scheduler) {
        return (observer) -> {
            SingleRegistration creg = new SingleRegistration();
            creg.set(scheduler.schedule(() -> {
                // FIXME safe register
                Registration reg = register(observer);
                creg.set(() -> {
                    scheduler.schedule(() -> {
                        reg.close();
                    });
                });
            }));
            return creg;
        };
    }
    /**
     * Wraps the current observer which captures
     * any exception during the register() method
     * call and immediately forwards it
     * to the observer's error() method.
     * @return the observer with safe registration
     */
    default Observable<T> registerSafe() {
        return (observer) -> {
            try {
                return register(observer);
            } catch (Throwable t) {
                observer.error(t);
                return Registration.EMPTY;
            }
        };
    }
    /**
     * Merges the incoming sequences of the sources into a single observable.
     * <p>Error condition in one of the sources is immediately propagated,
     * the last finish will terminate the entire observation.</p>
     * FIXME not sure about the race condition between the registration's
     * finish and the inner error.
     * @param <T>
     * @param sources
     * @return 
     */
    public static <T> Observable<T> merge(Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            AtomicInteger wip = new AtomicInteger(1);

            Runnable complete = () -> {
                if (wip.decrementAndGet() == 0 && !creg.isClosed()) {
                    try {
                        observer.finish();
                    } finally {
                        creg.close();
                    }
                }
            };
            
            Observer<T> so = Observer.createSafe(
                (v) -> {
                    observer.next(v);
                },
                (e) -> {
                    try {
                        observer.error(e);
                    } finally {
                        creg.close();
                    }
                },
                complete
            );
            
            sources.forEach(o -> {
                if (!creg.isClosed()) {
                    wip.incrementAndGet();
                    creg.add(o.register(so));
                }
            });

            complete.run();
            
            return creg;
        };
    }
    /**
     * Merges a dynamic sequence of observables.
     * <p>Error condition coming through any source observable is
     * forwarded immediately and the registrations are terminated.</p>
     * 
     * @param <T>
     * @param sources
     * @return 
     */
    public static <T> Observable<T> merge(Observable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            CompositeRegistration reg = new CompositeRegistration();

            AtomicInteger wip = new AtomicInteger(1);

            Runnable complete = () -> {
                if (wip.decrementAndGet() == 0 && !reg.isClosed()) {
                    try {
                        observer.finish();
                    } finally {
                        reg.close();
                    }
                }
            };

            Observer<Observable<? extends T>> sourceObserver = Observer.createSafe(
                (o) -> {
                    wip.incrementAndGet();

                    SingleRegistration itemReg = new SingleRegistration();
                    
                    reg.add(itemReg);
                    
                    Observer<T> itemObserver = Observer.createSafe(
                        (v) -> {
                            observer.next(v);
                        },
                        (e) -> {
                            try {
                                observer.error(e);
                            } finally {
                                reg.close();
                            }
                        },
                        () -> {
                            reg.remove(itemReg);
                            complete.run();
                        }
                    );

                    itemReg.set(o.register(itemObserver));
                },
                (e) -> {
                    try {
                        observer.error(e);
                    } finally {
                        reg.close();
                    }
                },
                complete
            );
            
            reg.add(sources.register(sourceObserver));
            
            return reg;
        };
    }
}
