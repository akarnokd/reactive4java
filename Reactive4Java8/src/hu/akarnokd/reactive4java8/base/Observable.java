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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
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
                    Subject<T, T> s = new DefaultObservable<>(true);
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
}
