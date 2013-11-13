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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
        try (Registration reg = register(Observer.createSafe(
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
    /**
     * Synchronously consumes the stream until the indexed predicate returns
     * false.
     * @param stoppableConsumer the indexed predicate/consumer
     */
    default void forEachWhile(IndexedPredicate<? super T> stoppableConsumer) {
        IntRef count = new IntRef();
        forEachWhile(v -> stoppableConsumer.test(count.value++, v));
    }
    /**
     * Synchronously consumes the stream until the predicate returns
     * false.
     * @param stoppableConsumer the predicate/consumer
     */
    default void forEachWhile(Predicate<? super T> stoppableConsumer) {
        CountDownLatch latch = new CountDownLatch(1);
        
        try (SingleRegistration reg = new SingleRegistration()) {
            reg.set(register(new DefaultObserver<T>(reg) {

                @Override
                protected void onNext(T value) {
                    if (!stoppableConsumer.test(value)) {
                        done();
                        latch.countDown();
                    }
                }

                @Override
                protected void onError(Throwable t) {
                    latch.countDown();
                }

                @Override
                protected void onFinish() {
                    latch.countDown();
                }
                
            }));
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // otherwise ignored
        }
    }
    /**
     * Projects the sequence of values of this observable via
     * the supplied indexed mapping function.
     * @param <U>
     * @param function
     * @return 
     */
    default <U> Observable<U> select(IndexedFunction<? super T, ? extends U> function) {
        return (observer) -> {
            IntRef count = new IntRef();
            return register(observer.compose(v -> function.apply(count.value++, v)));
        };
    }
    /**
     * Filters the sequence of values of this observable via
     * the supplied indexed predicate function.
     * @param predicate
     * @return 
     */
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
     * Registers the given observer safely with this observable by
     * sending any exception of the register method to the
     * observer's error() method.
     * @param observer
     * @return 
     */
    default Registration registerSafe(Observer<? super T> observer) {
        Objects.requireNonNull(observer);
        try {
            return register(observer);
        } catch (Throwable t) {
            observer.error(t);
            return Registration.EMPTY;
        }
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
    @SafeVarargs
    public static <T> Observable<T> merge(Observable<? extends T>... sources) {
        return merge(Arrays.asList(sources));
    }
    /**
     * Concatenates the values of the source sequences
     * one after another.
     * @param <T>
     * @param sources
     * @return
     */
    public static <T> Observable<T> concat(Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            Iterator<? extends Observable<? extends T>> it = sources.iterator();
            
            SingleRegistration sreg = new SingleRegistration();
            
            Observer<T> tobs = new Observer<T>() {
                boolean done;
                @Override
                public void next(T value) {
                    if (!done) {
                        observer.next(value);
                    }
                }
                
                @Override
                public void error(Throwable t) {
                    if (!done) {
                        done = true;
                        observer.error(t);
                    }
                }
                
                @Override
                public void finish() {
                    if (!done) {
                        if (it.hasNext()) {
                            if (!sreg.isClosed()) {
                                sreg.set(it.next().register(this));
                            }
                        } else {
                            done = true;
                            try {
                                observer.finish();
                            } finally {
                                sreg.close();
                            }
                        }
                    }
                }
            }.toThreadSafe();
            
            tobs.finish(); // trigger first item from sources
            
            return sreg;
        };
    }
    @SafeVarargs
    public static <T> Observable<T> concat(Observable<? extends T>... sources) {
        return concat(Arrays.asList(sources));
    }
    /**
     * Concatenates a dynamic sequence of observable sequences.
     * FIXME highly complicated concurrency mode, not sure
     * @param <T>
     * @param sources
     * @return
     */
    public static <T> Observable<T> concat(Observable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            BlockingQueue<Observable<? extends T>> queue = new LinkedBlockingQueue<>();
            
            AtomicInteger wip = new AtomicInteger(1);
            
            CompositeRegistration creg = new CompositeRegistration();
            
            SingleRegistration sreg = new SingleRegistration();
            
            Observer<T> tobs = new Observer<T>() {
                boolean done;
                @Override
                public void next(T value) {
                    if (!done) {
                        observer.next(value);
                    }
                }
                
                @Override
                public void error(Throwable t) {
                    if (!done) {
                        done = true;
                        try {
                            observer.error(t);
                        } finally {
                            creg.close();
                        }
                    }
                }
                
                @Override
                public void finish() {
                    if (!done) {
                        if (wip.decrementAndGet() == 0) {
                            done = true;
                            if (!creg.isClosed()) {
                                try {
                                    observer.finish();
                                } finally {
                                    creg.close();
                                }
                            }
                        } else {
                            Observable<? extends T> o = queue.poll();
                            if (o != null) {
                                sreg.set(o.register(this));
                            }
                        }
                    }
                }
            }.toThreadSafe();
            
            Observer<Observable<? extends T>> sourceObserver = new Observer<Observable<? extends T>>() {
                boolean done;
                @Override
                public void next(Observable<? extends T> value) {
                    if (!done) {
                        queue.add(value);
                        if (wip.incrementAndGet() == 2) {
                            wip.incrementAndGet();
                            tobs.finish();
                        }
                    }
                }
                
                @Override
                public void error(Throwable t) {
                    if (!done) {
                        done = true;
                        try {
                            observer.error(t);
                        } finally {
                            creg.close();
                        }
                    }
                }
                
                @Override
                public void finish() {
                    if (!done) {
                        done = true;
                        if (wip.decrementAndGet() == 0) {
                            if (!creg.isClosed()) {
                                try {
                                    observer.finish();
                                } finally {
                                    creg.close();
                                }
                            }
                        }
                    }
                }
                
            }.toThreadSafe();
            
            creg.add(sreg);
            creg.add(sources.register(sourceObserver));
            
            return creg;
        };
    }
    /**
     * Returns an observable sequence of the given range of integer values.
     * @param start the start of the value range, inclusive
     * @param end the end of the value range, exclusive
     * @return the observable
     */
    public static Observable<Integer> range(int start, int end) {
        return (observer) -> {
            int delta = (end >= start) ? 1 : -1;
            int count = delta * (end - start);
            int value = start;
            try {
                while (count-- > 0) {
                    observer.next(value);
                    value += delta;
                }
                observer.finish();
            } catch (Throwable t) {
                observer.error(t);
            }
            return Registration.EMPTY;
        };
    }
    /**
     * Returns an observable sequence of the given range of integer values.
     * @param start the start of the value range, inclusive
     * @param end the end of the value range, exclusive
     * @return the observable
     */
    public static Observable<Long> range(long start, long end) {
        return (observer) -> {
            int delta = (end >= start) ? 1 : -1;
            long count = delta * (end - start);
            long value = start;
            try {
                while (count-- > 0) {
                    observer.next(value);
                    value += delta;
                }
                observer.finish();
            } catch (Throwable t) {
                observer.error(t);
            }
            return Registration.EMPTY;
        };
    }
    /**
     * Delays the delivery of all events to the observers.
     * @param time
     * @param unit
     * @param scheduler
     * @return 
     */
    default Observable<T> delay(long time, TimeUnit unit, Scheduler scheduler) {
        return (observer) -> {
            SingleLaneScheduling sls = new SingleLaneScheduling(scheduler);
            
            CompositeRegistration creg = new CompositeRegistration();

            Observer<T> tobs = new Observer<T>() {
                @Override
                public void next(T value) {
                    SingleRegistration sreg = new SingleRegistration();
                    creg.add(sreg);
                    sreg.set(scheduler.schedule(time, unit, () -> {
                        sreg.set(sls.schedule(() -> {
                            creg.remove(sreg);
                            observer.next(value);
                        }));
                    }));
                }
                @Override
                public void error(Throwable t) {
                    SingleRegistration sreg = new SingleRegistration();
                    creg.add(sreg);
                    sreg.set(scheduler.schedule(time, unit, () -> {
                        sreg.set(sls.schedule(() -> {
                            creg.remove(sreg);
                            observer.error(t);
                        }));
                    }));
                }
                @Override
                public void finish() {
                    SingleRegistration sreg = new SingleRegistration();
                    creg.add(sreg);
                    sreg.set(scheduler.schedule(time, unit, () -> {
                        sreg.set(sls.schedule(() -> {
                            creg.remove(sreg);
                            observer.finish();
                        }));
                    }));
                }
            };
            
            creg.add(register(tobs));
            return creg;
        };
    }
    /**
     * Throttles the stream of values by sending out the last observed
     * value only after the given timeout period.
     * @param time
     * @param unit
     * @param scheduler
     * @return 
     */
    default Observable<T> throttle(long time, TimeUnit unit, Scheduler scheduler) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            SingleRegistration sreg = new SingleRegistration();
            
            Observer<T> tobs = new DefaultObserver<T>(creg) {
                @Override
                public void onNext(T value) {
                    sreg.set(scheduler.schedule(time, unit, () -> {
                        try {
                            ls.sync(() -> observer.next(value));
                        } catch (Throwable t) {
                            error(t);
                        }
                    }));
                }
                @Override
                public void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                public void onFinish() {
                    observer.finish();
                }
            };
            creg.add(sreg);
            creg.add(register(tobs));
            return creg;
        };
    }
    /**
     * Combines the pair of elements from this observable and
     * the other iterable sequence and applies the given combiner function.
     * @param <U>
     * @param <V>
     * @param other
     * @param function
     * @return 
     */
    default <U, V> Observable<V> zip(Iterable<? extends U> other, 
            BiFunction<? super T, ? super U, ? extends V> function) {
        return (observer) -> {
            Iterator<? extends U> it = other.iterator();
            if (it.hasNext()) {
                SingleRegistration sreg = new SingleRegistration();

                Observer<T> tobs = new DefaultObserver<T>(sreg) {
                    @Override
                    public void onNext(T value) {
                        U u = it.next();
                        observer.next(function.apply(value, u));
                        if (!it.hasNext()) {
                            finish();
                        }
                    }
                    @Override
                    public void onError(Throwable t) {
                        observer.error(t);
                    }
                    @Override
                    public void onFinish() {
                        observer.finish();
                    }
                };

                sreg.set(register(tobs));
                return sreg;
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Combines the pairs of values of this and the other observable
     * sequence and applies the given function.
     * @param <U>
     * @param <V>
     * @param other
     * @param function
     * @return 
     */
    default <U, V> Observable<V> zip(Observable<? extends U> other, 
            BiFunction<? super T, ? super U, ? extends V> function) {
        return (observer) -> {
                CompositeRegistration creg = new CompositeRegistration();
                
                Lock shared = new ReentrantLock();
                
                BoolRef cdone = new BoolRef();
                
                Queue<T> tqueue = new LinkedList<>();
                Queue<U> uqueue = new LinkedList<>();
                
                Observer<T> tobs = new DefaultObserver<T>(creg, shared) {
                    @Override
                    protected void onNext(T value) {
                        if (!cdone.value) {
                            if (uqueue.isEmpty()) {
                                tqueue.add(value);
                            } else {
                                U u = uqueue.poll();
                                observer.next(function.apply(value, u));
                            }
                        }
                    }
                    @Override
                    protected void onError(Throwable t) {
                        if (!cdone.value) {
                            cdone.value = true;
                            observer.error(t);
                        }
                    }
                    @Override
                    protected void onFinish() {
                        if (!cdone.value) {
                            if (tqueue.isEmpty() && uqueue.isEmpty()) {
                                cdone.value = true;
                                observer.finish();
                            }
                        }
                    }
                };
                Observer<U> uobs = new DefaultObserver<U>(creg, shared) {
                    @Override
                    protected void onNext(U value) {
                        if (!cdone.value) {
                            if (tqueue.isEmpty()) {
                                uqueue.add(value);
                            } else {
                                T t = tqueue.poll();
                                observer.next(function.apply(t, value));
                            }
                        }
                    }
                    @Override
                    protected void onError(Throwable t) {
                        if (!cdone.value) {
                            cdone.value = true;
                            observer.error(t);
                        }
                    }
                    @Override
                    protected void onFinish() {
                        if (!cdone.value) {
                            if (tqueue.isEmpty() && uqueue.isEmpty()) {
                                cdone.value = true;
                                observer.finish();
                            }
                        }
                    }
                };
                
                creg.add(register(tobs));
                creg.add(other.register(uobs));
                
                return creg;
            };
    }
    default Observable<T> concatWith(Observable<? extends T> other) {
        return concat(this, other);
    }
    default Observable<T> mergeWith(Observable<? extends T> other) {
        return merge(this, other);
    }
    /**
     * Creates an iterable sequence which synchronously
     * waits for events from this observable.
     * The iterator of the returned iterable implements
     * the Registration interface, therefore, the registration
     * can be closed.
     * @return 
     */
    default Iterable<T> toIterable() {
        return () -> {
            BlockingQueue<Optional<T>> queue = new LinkedBlockingQueue<>();

            SingleRegistration sreg = new SingleRegistration();
            
            sreg.set(register(Observer.create(
                (v) -> queue.add(Optional.of(v)),
                (e) -> queue.add(Optional.empty()),
                () -> Optional.empty()
            )));
            
            return new IteratorRegistered<T>() {
                /** Indicates that there is an untaken value. */
                boolean has;
                /** Indicator that the source observable finished. */
                boolean done;
                /** The value for next. */
                T value;
                @Override
                public boolean hasNext() {
                    if (!has) {
                        if (!done) {
                            try {
                                Optional<T> v = queue.take();
                                if (v.isPresent()) {
                                    has = true;
                                } else {
                                    done = true;
                                }
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                done = true;
                            }
                        }
                    }
                    return has;
                }
                @Override
                public T next() {
                    if (has || hasNext()) {
                        has = false;
                        T v = value;
                        value = null;
                        return v;
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void close() {
                    sreg.close();
                }
                
            };
        };
    }
    /**
     * Creates an observable sequence which emits increasing values from
     * zero after each period of time.
     * @param period
     * @param unit
     * @param scheduler
     * @return 
     */
    public static Observable<Long> tick(long period, TimeUnit unit, 
            Scheduler scheduler) {
        return (observer) -> {
            LongRef count = new LongRef();
            return scheduler.schedule(period, period, unit, () -> {
                observer.next(count.value++);
            });
        };
    }
    /**
     * Returns an observable sequence which buffers the values
     * of this observable into the specified chunks.
     * <p>Does not emit empty buffers.</p>
     * @param count
     * @return 
     */
    default Observable<List<T>> buffer(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count > 0!");
        }
        return (observer) -> {
            Ref<List<T>> ref = new Ref<>();
            
            return register(Observer.create(
                (v) -> {
                    if (ref.value == null) {
                        ref.value = new ArrayList<>(count);
                    }
                    ref.value.add(v);
                    if (ref.value.size() == count) {
                        List<T> list = ref.value;
                        ref.value = null;
                        observer.next(list);
                    }
                },
                (e) -> {
                    observer.error(e);
                },
                () -> {
                    if (ref.value != null) {
                        List<T> list = ref.value;
                        ref.value = null;
                        observer.next(list);
                    }
                    observer.finish();
                }
            ));
        };
    }
    /**
     * Returns an observable sequence of potentially overlapping
     * windows opened by the opening observable and closed by
     * the observable returned by the windowClosingSelector function.
     * @param <U>
     * @param <V>
     * @param opening
     * @param windowClosingSelector
     * @return 
     */
    default <U, V> Observable<Observable<T>> window(Observable<U> opening, 
            Function<? super U, ? extends Observable<V>> windowClosingSelector) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            Lock lock = new ReentrantLock();

            Map<Unique<U>, Subject<T, T>> openWindows = new LinkedHashMap<>();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(creg, lock) {
                @Override
                protected void onNext(T value) {
                    openWindows.values().forEach(s -> s.next(value));
                }
                @Override
                protected void onError(Throwable t) {
                    openWindows.values().forEach(s -> s.error(t));
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    openWindows.values().forEach(Observer::finish);
                    observer.finish();
                }
            };

            SingleRegistration sureg = new SingleRegistration();
            
            DefaultObserver<U> uobs = new DefaultObserver<U>(sureg, lock) {
                @Override
                protected void onNext(U value) {
                    Unique<U> token = Unique.of(value);
                    Subject<T, T> s = new DefaultObservable<>();
                    openWindows.put(token, s);
                    
                    Observable<V> closing = windowClosingSelector.apply(value);
                    
                    SingleRegistration sreg = new SingleRegistration();
                    creg.add(sreg);
                    
                    sreg.set(closing.register(new DefaultObserver<V>(sreg, lock) {
                        @Override
                        protected void onNext(V value) {
                            // ignored
                        }
                        @Override
                        protected void onError(Throwable t) {
                            tobs.error(t);
                        }
                        @Override
                        protected void onFinish() {
                            Subject<T, T> s = openWindows.remove(token);
                            s.finish();
                            creg.remove(sreg);
                        }
                    }));
                    observer.next(s);
                }
                @Override
                protected void onError(Throwable t) {
                    tobs.error(t);
                }
                @Override
                protected void onFinish() {
                    // FIXME no more windows, do we care?
                }
            };

            creg.add(sureg);
            creg.add(register(tobs));
            sureg.set(opening.register(uobs));
            
            return creg;
        };
    }
    /**
     * Buffers the values of this observable according to the windows
     * defined by the time and unit.
     * <p>Does not emit empty buffers.</p>
     * @param time
     * @param unit
     * @param scheduler
     * @return 
     */
    default Observable<List<T>> buffer(long time, TimeUnit unit, Scheduler scheduler) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();

            Lock lock = new ReentrantLock();
            LockSync ls = new LockSync(lock);
            
            Ref<List<T>> ref = new Ref<>();

            DefaultObserver<T> tobs = new DefaultObserver<T>(creg, lock) {
                @Override
                protected void onNext(T value) {
                    if (ref.value == null) {
                        ref.value = new ArrayList<>();
                    }
                    ref.value.add(value);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    if (ref.value != null) {
                        List<T> list = ref.value;
                        ref.value = null;
                        observer.next(list);
                    }
                    observer.finish();
                }
            };
            
            creg.add(register(tobs));

            creg.add(scheduler.schedule(time, time, unit, () -> {
                ls.sync(() -> {
                    if (ref.value != null && !tobs.isDone()) {
                        List<T> list = ref.value;
                        ref.value = null;
                        observer.next(list);
                    }
                });
            }));
            return creg;
        };
    }
    /**
     * Buffers values of potentially overlapping windows opened
     * by the windowOpening sequence and closed by another
     * observable sequence selected for the given windowOpening value.
     * @param <U>
     * @param <V>
     * @param opening
     * @param windowClosingSelector
     * @return 
     */
    default <U, V> Observable<List<T>> buffer(
            Observable<U> opening,
            Function<? super U, ? extends Observable<V>> windowClosingSelector) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            Lock lock = new ReentrantLock();

            Map<Unique<U>, List<T>> openWindows = new LinkedHashMap<>();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(creg, lock) {
                @Override
                protected void onNext(T value) {
                    openWindows.values().forEach(s -> s.add(value));
                }
                @Override
                protected void onError(Throwable t) {
                    openWindows.values().forEach(s -> observer.next(s));
                    openWindows.clear();
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    openWindows.values().forEach(s -> observer.next(s));
                    openWindows.clear();
                    observer.finish();
                }
            };

            SingleRegistration sureg = new SingleRegistration();
            
            DefaultObserver<U> uobs = new DefaultObserver<U>(sureg, lock) {
                @Override
                protected void onNext(U value) {
                    Unique<U> token = Unique.of(value);
                    List<T> s = new ArrayList<>();
                    openWindows.put(token, s);
                    
                    Observable<V> closing = windowClosingSelector.apply(value);
                    
                    SingleRegistration sreg = new SingleRegistration();
                    creg.add(sreg);
                    
                    sreg.set(closing.register(new DefaultObserver<V>(sreg, lock) {
                        @Override
                        protected void onNext(V value) {
                            // ignored
                        }
                        @Override
                        protected void onError(Throwable t) {
                            tobs.error(t);
                        }
                        @Override
                        protected void onFinish() {
                            observer.next(openWindows.remove(token));
                            observer.finish();
                            creg.remove(sreg);
                        }
                    }));
                }
                @Override
                protected void onError(Throwable t) {
                    tobs.error(t);
                }
                @Override
                protected void onFinish() {
                    // FIXME no more windows, do we care?
                }
            };

            creg.add(sureg);
            creg.add(register(tobs));
            sureg.set(opening.register(uobs));
            
            return creg;
        };
       
    }
    
    /**
     * Combines the latest observed values from the source
     * observables and emits a result produced via the function.
     * @param <T>
     * @param <U>
     * @param <V>
     * @param first
     * @param second
     * @param function
     * @return 
     */
    public static <T, U, V> Observable<V> combineLatest(
            Observable<T> first,
            Observable<U> second,
            BiFunction<? super T, ? super U, ? extends V> function) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            Lock lock = new ReentrantLock();
            Ref<T> t = new Ref<>();
            BoolRef tf = new BoolRef();
            Ref<U> u = new Ref<>();
            BoolRef uf = new BoolRef();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(creg, lock) {
                @Override
                protected void onNext(T value) {
                    tf.value = true;
                    t.value = value;
                    if (uf.value) {
                        observer.next(function.apply(value, u.value));
                    }
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    observer.finish();
                }
                
            };
            DefaultObserver<U> uobs = new DefaultObserver<U>(creg, lock) {
                @Override
                protected void onNext(U value) {
                    uf.value = true;
                    u.value = value;
                    if (tf.value) {
                        observer.next(function.apply(t.value, value));
                    }
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    observer.finish();
                }
            };
            
            creg.add(first.register(tobs));
            creg.add(second.register(uobs));
            
            return creg;
        };
    }
    /**
     * Returns an observable which counts from start to start+count-1
     * by emitting values in the given periodicity.
     * @param start
     * @param count
     * @param period
     * @param unit
     * @param scheduler
     * @return 
     */
    public static Observable<Long> tick(long start, long count, 
            long period, TimeUnit unit, Scheduler scheduler) {
        if (count == 0) {
            return empty();
        }
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            LongRef counter = LongRef.of(start);
            
            sreg.set(scheduler.schedule(period, period, unit, () -> {
                observer.next(counter.value++);
                if (counter.value == start + count) {
                    observer.finish();
                    sreg.close();
                }
            }));
            
            return sreg;
        };
    }
    /**
     * Returns an observable which counts from start to start+count-1
     * by emitting values in the given periodicity after the initial delay.
     * @param start
     * @param count
     * @param initialDelay
     * @param period
     * @param unit
     * @param scheduler
     * @return 
     */
    public static Observable<Long> tick(long start, long count, 
            long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            LongRef counter = LongRef.of(start);
            
            sreg.set(scheduler.schedule(initialDelay, period, unit, () -> {
                observer.next(counter.value++);
                if (counter.value == start + count) {
                    observer.finish();
                    sreg.close();
                }
            }));
            
            return sreg;
        };
    }
    /**
     * Returns a single observable boolean value if all
     * elements of this observable satisfy the given predicate.
     * @param predicate
     * @return 
     */
    default Observable<Boolean> all(Predicate<? super T> predicate) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            
            sreg.set(register(new DefaultObserver<T>(sreg) {
                boolean has;
                boolean result;
                @Override
                protected void onNext(T value) {
                    has = true;
                    if (!predicate.test(value)) {
                        result = false;
                        finish();
                    } else {
                        result = true;
                    }
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    if (has) {
                        observer.next(result);
                    }
                    observer.finish();
                }
            }));
            
            return sreg;
        };
    }
    /**
     * Returns an observable sequence which immediately throws
     * a RuntimeException.
     * @param <T>
     * @return 
     */
    public static <T> Observable<T> throwError() {
        return throwError(() -> new RuntimeException());
    }
    /**
     * Returns an observable sequence which throws an exception
     * supplied by a factory.
     * @param <T>
     * @param exceptionFactory
     * @return 
     */
    public static <T> Observable<T> throwError(
            Supplier<? extends Exception> exceptionFactory) {
        return (observer) -> {
            observer.error(exceptionFactory.get());
            return Registration.EMPTY;
        };
    }
    /**
     * Returns an observable with a single true value if the current
     * observable's any element matches the given predicate.
     * @param predicate
     * @return 
     */
    default Observable<Boolean> any(Predicate<? super T> predicate) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(sreg) {
                /** There was at least a single value. */
                boolean has;
                /** The result to emit. */
                boolean result;
                @Override
                protected void onNext(T value) {
                    has = true;
                    if (predicate.test(value)) {
                        result = true;
                        finish();
                    }
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    if (has) {
                        try {
                            observer.next(result);
                        } catch (Throwable t) {
                            error(t);
                        }
                    }
                    observer.finish();
                }
                
            };
            
            sreg.set(register(tobs));
            
            return sreg;
        };
    }
    /**
     * Returns the first value of this observable sequence or
     * the Optional.empty().
     * @return 
     */
    default Optional<T> first() {
        BoolRef has = new BoolRef();
        Ref<T> ref = new Ref<>();
        forEachWhile(v -> {
            has.value = true;
            ref.value = v;
            return false;
        });
        if (has.value) {
            return Optional.of(ref.value);
        }
        return Optional.empty();
    }
    /**
     * Returns the last value of this observable sequence or
     * the Optional.empty().
     * @return 
     */
    default Optional<T> last() {
                BoolRef has = new BoolRef();
        Ref<T> ref = new Ref<>();
        forEach(v -> {
            has.value = true;
            ref.value = v;
        });
        if (has.value) {
            return Optional.of(ref.value);
        }
        return Optional.empty();
    }
    /**
     * Aggregates the values of this observable by using
     * an aggregator function for intermediate values and
     * a divider function for the final operation.
     * <p>Note that in case the source is empty, the
     * divider function receives the seed and zero index.</p>
     * @param <U>
     * @param <V>
     * @param seed
     * @param aggregator
     * @param divider
     * @return 
     */
    default <U, V> Observable<V> aggregate(
            U seed, 
            BiFunction<? super U, ? super T, ? extends U> aggregator,
            IndexedFunction<? super U, ? extends V> divider) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                U accumulator = seed;
                int count;
                @Override
                protected void onNext(T value) {
                    count++;
                    accumulator = aggregator.apply(accumulator, value);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    try {
                        observer.next(divider.apply(count, accumulator));
                    } catch (Throwable t) {
                        error(t);
                        return;
                    }
                    observer.finish();
                }
            };
            
            return register(tobs);
        };
    }
    /**
     * Scans this observable and applies an aggregator function
     * with each element, and the intermediate values are 
     * emitted.
     * <p>Empty source yields empty observable.</p>
     * @param <U>
     * @param seed
     * @param aggregator
     * @return 
     */
    default <U> Observable<U> scan(U seed, 
            BiFunction<? super U, ? super T, ? extends U> aggregator) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                U accumulator = seed;
                @Override
                protected void onNext(T value) {
                    accumulator = aggregator.apply(accumulator, value);
                    observer.next(accumulator);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    observer.finish();
                }
            };
            return register(tobs);
        };
    }
    /**
     * Aggregates the the sequence of this observable with the given
     * aggregator function and returns the last aggregated value.
     * @param <U>
     * @param seed
     * @param aggregator
     * @return 
     */
    default <U> Observable<U> aggregate(U seed,
            BiFunction<? super U, ? super T, ? extends U> aggregator) {
        return aggregate(seed, aggregator, (i, v) -> v);
    }
    /**
     * Aggregate this observable sequence with the given aggregator function.
     * <p>If this observable is empty, the resulting observable is also
     * empty. If this observable contains only one element, that
     * is returned only.</p>
     * @param aggregator
     * @return 
     */
    default Observable<T> aggregate(
            BiFunction<? super T, ? super T, ? extends T> aggregator) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                T accumulator;
                int count;
                @Override
                protected void onNext(T value) {
                    count++;
                    if (count == 1) {
                        accumulator = value;
                    } else {
                        accumulator = aggregator.apply(accumulator, value);
                    }
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    if (count > 0) {
                        try {
                            observer.next(accumulator);
                        } catch (Throwable t) {
                            error(t);
                            return;
                        }
                    }
                    observer.finish();
                }
            };
            return register(tobs);
        };
    }
    /**
     * Aggregates this source sequence and emits the intermediate values.
     * @param aggregator
     * @return 
     */
    default Observable<T> scan(
            BiFunction<? super T, ? super T, ? extends T> aggregator) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                T accumulator;
                int count;
                @Override
                protected void onNext(T value) {
                    count++;
                    if (count == 1) {
                        accumulator = value;
                    } else {
                        accumulator = aggregator.apply(accumulator, value);
                    }
                    observer.next(accumulator);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    observer.finish();
                }
            };
            return register(tobs);
        };
    }
    /**
     * Channels the events of the observable which fires first.
     * @param <T>
     * @param sources
     * @return 
     */
    public static <T> Observable<T> ambiguous(
            Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            
            CompositeRegistration creg = new CompositeRegistration();
            SingleRegistration sreg = new SingleRegistration(creg);
            
            AtomicReference<Object> winner = new AtomicReference<>();
            
            sources.forEach(o -> {
                SingleRegistration srego = new SingleRegistration();
                creg.add(srego);
                srego.set(o.register(new DefaultObserver<T>() {
                    boolean once;
                    boolean wewon;
                    @Override
                    protected void onNext(T value) {
                        if (check()) {
                            observer.next(value);
                        }
                    }
                    @Override
                    protected void onError(Throwable t) {
                        if (check()) {
                            observer.error(t);
                        }
                    }
                    @Override
                    protected void onFinish() {
                        if (check()) {
                            observer.finish();
                        }
                    }
                    /**
                     * Check and set the winner, then cancel the others.
                     * @return 
                     */
                    boolean check() {
                        if (!once) {
                            once = true;
                            if (winner.get() == null) {
                                wewon = winner.compareAndSet(null, this);
                            } else {
                                wewon = winner.get() == this;
                            }
                            if (wewon) {
                                creg.remove(srego);
                                sreg.set(srego);
                            }
                        }
                        return wewon;
                    }
                }));
            });
            
            return sreg;
        };
    }
    /**
     * Channels the events of the observable which fires first.
     * @param <T>
     * @param sources
     * @return 
     */
    @SafeVarargs
    public static <T> Observable<T> ambiguous(
            Observable<? extends T>... sources) {
        return ambiguous(Arrays.asList(sources));
    }
    /**
     * Returns the number of elements in this observable.
     * @return 
     */
    default Observable<Integer> count() {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                /** The counter. */
                int count;
                @Override
                protected void onNext(T value) {
                    count++;
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    try {
                        observer.next(count);
                    } catch (Throwable t) {
                        error(t);
                        return;
                    }
                    observer.finish();
                }
            };
            return register(tobs);
        };
    }
    /**
     * Returns the number of elements in this observable.
     * @return 
     */
    default Observable<Long> countLong() {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                /** The counter. */
                long count;
                @Override
                protected void onNext(T value) {
                    count++;
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    try {
                        observer.next(count);
                    } catch (Throwable t) {
                        error(t);
                        return;
                    }
                    observer.finish();
                }
            };
            return register(tobs);
        };
    }
    /**
     * Groups the values in this observable according to the key
     * selector.
     * @param <K>
     * @param keyExtractor
     * @return 
     */
    default <K> Observable<GroupedObservable<T, K>> groupBy(
            Function<? super T, ? extends K> keyExtractor
    ) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                /** The current groups. */
                Map<K, DefaultGroupedObservable<T, K>> groups = new LinkedHashMap<>();
                @Override
                protected void onNext(T value) {
                    K key = keyExtractor.apply(value);
                    DefaultGroupedObservable<T, K> g = groups.computeIfAbsent(key, (k) -> {
                        DefaultGroupedObservable<T, K> g2 = new DefaultGroupedObservable<>(k);
                        observer.next(g2);
                        return g2;
                    });
                    g.next(value);
                }
                @Override
                protected void onError(Throwable t) {
                    groups.values().forEach(g -> g.error(t));
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    groups.values().forEach(Observer::finish);
                    observer.finish();
                }
            };
            
            return register(tobs);
        };
    }
    /**
     * Groups the values of this observable according to the given
     * key selector and each group has its duration given
     * by another observable.
     * @param <K>
     * @param <D>
     * @param keySelector
     * @param durationSelector
     * @return 
     */
    default <K, D> Observable<GroupedObservable<T, K>> groupByUnitl(
            Function<? super T, ? extends K> keySelector,
            Function<? super GroupedObservable<T, K>, ? extends Observable<D>> durationSelector
    ) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(creg) {
                Map<K, DefaultGroupedObservable<T, K>> groups = new LinkedHashMap<>();
                @Override
                protected void onNext(T value) {
                    K key = keySelector.apply(value);
                    DefaultGroupedObservable<T, K> g = groups.computeIfAbsent(key, (k) -> {
                        DefaultGroupedObservable<T, K> g2 = new DefaultGroupedObservable<>(k);
                        observer.next(g2);
                        
                        SingleRegistration sreg = new SingleRegistration();
                        creg.add(sreg);
                        
                        Observable<D> endGroup = durationSelector.apply(g2);
                        
                        sreg.set(endGroup.register(Observer.create(
                             () -> {
                                 ls.sync(() -> {
                                     creg.remove(sreg);
                                     groups.remove(key).finish();
                                 });
                             },
                             (e) -> innerError(e)
                        )));
                        
                        return g2;
                    });
                    g.next(value);
                }
                /**
                 * Forward an inner error to an outer error.
                 * @param t 
                 */
                protected void innerError(Throwable t) {
                    error(t);
                }
                @Override
                protected void onError(Throwable t) {
                    groups.values().forEach(g -> g.error(t));
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    groups.values().forEach(Observer::finish);
                    observer.finish();
                }
            };
            
            return register(tobs);
        };
    }
    /**
     * Correlates two observable sequences and applies a
     * function to their value when they overlap.
     * <p>Durations are determined by either a next or finish call.</p>
     * @param <L> the value type of the left sequence
     * @param <R> the value type of the right sequence
     * @param <LD> the duration type of the left sequence
     * @param <RD> the duration type of the right sequence
     * @param <V> the result value type
     * @param left
     * @param right
     * @param leftDuration
     * @param rightDuration
     * @param resultSelector
     * @return 
     */
    public static <L, R, LD, RD, V> Observable<V> join(
            Observable<? extends L> left,
            Observable<? extends R> right,
            Function<? super L, ? extends Observable<LD>> leftDuration,
            Function<? super R, ? extends Observable<RD>> rightDuration,
            BiFunction<? super L, ? super R, ? extends V> resultSelector
    ) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            Lock lock = new ReentrantLock();
            
            BoolRef leftDone = new BoolRef();
            IntRef leftIdx = new IntRef();
            Map<Integer, L> leftValues = new LinkedHashMap<>();
            SingleRegistration leftReg = new SingleRegistration();

            BoolRef rightDone = new BoolRef();
            IntRef rightIdx = new IntRef();
            Map<Integer, R> rightValues = new LinkedHashMap<>();
            SingleRegistration rightReg = new SingleRegistration();

            creg.add(leftReg);
            creg.add(rightReg);
            
            DefaultObserver<L> lobs = new DefaultObserver<L>(leftReg, lock) {
                void expire(int id, Registration reg) {
                    ls.sync(() -> {
                       if (leftValues.containsKey(id)) {
                           leftValues.remove(id);
                           if (leftValues.isEmpty() && leftDone.value) {
                               observer.finish();
                               creg.close();
                           }
                       } 
                    });
                    creg.remove(reg);
                    reg.close();
                }
                @Override
                protected void onNext(L value) {
                    int id = leftIdx.value++;
                    leftValues.put(id, value);
                    
                    SingleRegistration sreg = new SingleRegistration();

                    Observable<LD> duration = leftDuration.apply(value);
                    
                    sreg.set(duration.register(Observer.create(
                        (t) -> expire(id, sreg),
                        (e) -> innerError(e),
                        () -> expire(id, sreg)
                    )));
                    
                    rightValues.values().forEach(r -> {
                        V v = resultSelector.apply(value, r);
                        observer.next(v);
                    });
                }
                void innerError(Throwable e) {
                    error(e);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                    creg.close();
                }
                @Override
                protected void onFinish() {
                    leftDone.value = true;
                    if (rightDone.value || leftValues.isEmpty()) {
                        observer.finish();
                        creg.close();
                    } else {
                        close();
                    }
                }
            };
            DefaultObserver<R> robs = new DefaultObserver<R>(rightReg, lock) {
                void expire(int id, Registration reg) {
                    ls.sync(() -> {
                       if (rightValues.containsKey(id)) {
                           rightValues.remove(id);
                           if (rightValues.isEmpty() && rightDone.value) {
                               observer.finish();
                               creg.close();
                           }
                       } 
                    });
                    creg.remove(reg);
                    reg.close();
                }
                @Override
                protected void onNext(R value) {
                    int id = rightIdx.value++;
                    rightValues.put(id, value);
                    
                    SingleRegistration sreg = new SingleRegistration();

                    Observable<RD> duration = rightDuration.apply(value);
                    
                    sreg.set(duration.register(Observer.create(
                        (t) -> expire(id, sreg),
                        (e) -> innerError(e),
                        () -> expire(id, sreg)
                    )));
                    
                    leftValues.values().forEach(l -> {
                        V v = resultSelector.apply(l, value);
                        observer.next(v);
                    });
                }
                void innerError(Throwable e) {
                    error(e);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                    creg.close();
                }
                @Override
                protected void onFinish() {
                    rightDone.value = true;
                    if (leftDone.value || rightValues.isEmpty()) {
                        observer.finish();
                        creg.close();
                    } else {
                        close();
                    }
                }
            };
            
            creg.add(left.register(lobs));
            creg.add(right.register(robs));
            
            return creg;
        };
    }
    /**
     * Repeats the given value indefinitely.
     * @param <T>
     * @param value
     * @return 
     */
    public static <T> Observable<T> repeat(T value) {
        return (observer) -> {
            while (!Thread.currentThread().isInterrupted()) {
                observer.next(value);
            }
//            observer.error(new InterruptedException());
            return Registration.EMPTY;
        };
    }
    /**
     * Repeats the given value count times.
     * @param <T>
     * @param value
     * @param count
     * @return 
     */
    public static <T> Observable<T> repeat(T value, int count) {
        return (observer) -> {
            for (int i = 0; i < count; i++) {
                observer.next(value);
            } 
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Repeatedly registers with this observable if finishes normally.
     * @param condition
     * @return 
     */
    default Observable<T> doWhile(BooleanSupplier condition) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();

            Observer<T> tobs = new Observer<T>() {
                @Override
                public void next(T value) {
                    observer.next(value);
                }
                @Override
                public void error(Throwable t) {
                    observer.error(t);
                    sreg.close();
                }
                @Override
                public void finish() {
                    if (condition.getAsBoolean()) {
                        sreg.set(register(this));
                    } else {
                        observer.finish();
                        sreg.close();
                    }
                }
            };
            
            sreg.set(register(tobs));
            
            return sreg;
        };
    }
    /**
     * Returns an observable sequence which registers
     * with all of the source observables even if an error
     * event occurs
     * @param sources
     * @return 
     */
    public static <T> Observable<T> resumeAlways(Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            Iterator<? extends Observable<? extends T>> it = sources.iterator();
            
            if (it.hasNext()) {
                SingleRegistration sreg = new SingleRegistration();
                Observer<T> tobs = new Observer<T>() {
                    @Override
                    public void next(T value) {
                        observer.next(value);
                    }
                    @Override
                    public void error(Throwable t) {
                        next();
                    }
                    @Override
                    public void finish() {
                        next();
                    }
                    /** Register with the next observable. */
                    void next() {
                        if (it.hasNext()) {
                            sreg.set(it.next().register(this));
                        } else {
                            observer.finish();
                            sreg.close();
                        }
                    }
                };
                
                sreg.set(it.next().register(tobs));
                
                return sreg;
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Returns an observable sequence which registers
     * with all of the source observables even if an error
     * event occurs
     * @param sources
     * @return 
     */
    public static <T> Observable<T> resumeOnError(Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            Iterator<? extends Observable<? extends T>> it = sources.iterator();
            
            if (it.hasNext()) {
                SingleRegistration sreg = new SingleRegistration();
                Observer<T> tobs = new Observer<T>() {
                    @Override
                    public void next(T value) {
                        observer.next(value);
                    }
                    @Override
                    public void error(Throwable t) {
                        next();
                    }
                    @Override
                    public void finish() {
                        observer.finish();
                        sreg.close();
                    }
                    /** Register with the next observable. */
                    void next() {
                        if (it.hasNext()) {
                            sreg.set(it.next().register(this));
                        } else {
                            observer.finish();
                            sreg.close();
                        }
                    }
                };
                
                sreg.set(it.next().register(tobs));
                
                return sreg;
            }
            observer.finish();
            return Registration.EMPTY;
        };
    }
}