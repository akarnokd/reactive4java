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


package hu.akarnokd.reactive4java8;

import hu.akarnokd.reactive4java8.registrations.CompositeRegistration;
import hu.akarnokd.reactive4java8.observables.DefaultGroupedObservable;
import hu.akarnokd.reactive4java8.observables.DefaultObservable;
import hu.akarnokd.reactive4java8.observers.DefaultObserver;
import hu.akarnokd.reactive4java8.observables.GroupedObservable;
import hu.akarnokd.reactive4java8.util.IteratorRegistered;
import hu.akarnokd.reactive4java8.observables.ObservableImpl;
import hu.akarnokd.reactive4java8.observables.ObservableRegistration;
import hu.akarnokd.reactive4java8.observers.SimpleObserver;
import hu.akarnokd.reactive4java8.schedulers.SingleLaneScheduling;
import hu.akarnokd.reactive4java8.registrations.SingleRegistration;
import hu.akarnokd.reactive4java8.util.LockSync;
import hu.akarnokd.reactive4java8.util.IndexedPredicate;
import hu.akarnokd.reactive4java8.util.IndexedConsumer;
import hu.akarnokd.reactive4java8.util.IndexedFunction;
import hu.akarnokd.reactive4java8.util.LongRef;
import hu.akarnokd.reactive4java8.util.Ref;
import hu.akarnokd.reactive4java8.util.BoolRef;
import hu.akarnokd.reactive4java8.util.IntRef;
import hu.akarnokd.reactive4java8.util.Timestamped;
import hu.akarnokd.reactive4java8.util.TimeInterval;
import hu.akarnokd.reactive4java8.util.Unique;
import hu.akarnokd.reactive4java8.subjects.AsyncSubject;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
            try {
                for (T t : values) {
                    observer.next(t);
                }
            } catch (Throwable t) {
                observer.error(t);
                return Registration.EMPTY;
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
            try {
                for (T t : src) {
                    observer.next(t);
                }
            } catch (Throwable t) {
                observer.error(t);
                return Registration.EMPTY;
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
        return (observer) -> {
            try {
                stream.forEach(observer::next);
            } catch (Throwable t) {
                observer.error(t);
                return Registration.EMPTY;
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
     * the observer is unregistered and the interrupted exception consumed.
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
    /**
     * Synchronously consumes the observable sequence and calls
     * the given indexed consumer with each value observed.
     * @param consumer 
     */
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
     * Returns an observable sequence which contains
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
    /**
     * Returns an observable sequence which immediately
     * fires a finish event.
     * @param <T>
     * @return
     */
    public static <T> Observable<T> empty() {
        return (observer) -> {
            observer.finish();
            return Registration.EMPTY;
        };
    }
    /**
     * Returns an observable sequence which never fires
     * any event.
     * @param <T>
     * @return
     */
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
            SingleRegistration sreg = new SingleRegistration();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(sreg) {
                int i;
                @Override
                protected void onNext(T value) {
                    observer.next(value);
                    if (++i == count) {
                        finish();
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
            
            sreg.set(registerSafe(tobs));
            
            return sreg;
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
            creg.add(register(Observer.create(
                    (T v) -> {
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
        return ObservableImpl.merge(sources);
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
        return ObservableImpl.merge(sources);
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
        return ObservableImpl.concat(sources);
    }
    /**
     * Concatenates the values of the source sequences
     * one after another.
     * @param <T>
     * @param sources
     * @return
     */
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
        return ObservableImpl.concat(sources);
    }
    /**
     * Returns an observable sequence of the given range of integer values.
     * @param start the start of the value range, inclusive
     * @param end the end of the value range, exclusive
     * @return the observable
     */
    public static Observable<Integer> range(int start, int end) {
        return ObservableImpl.range(start, end);
    }
    /**
     * Returns an observable sequence of the given range of integer values.
     * @param start the start of the value range, inclusive
     * @param end the end of the value range, exclusive
     * @return the observable
     */
    public static Observable<Long> range(long start, long end) {
        return ObservableImpl.range(start, end);
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
                
                @Override
                protected void finalize() throws Throwable {
                    try {
                        sreg.close();
                    } finally {
                        super.finalize();
                    }
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
        return ObservableImpl.tick(period, unit, scheduler);
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
        return ObservableImpl.combineLatest(first, second, function);
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
        return ObservableImpl.tick(start, count, period, unit, scheduler);
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
        return ObservableImpl.tick(start, count, initialDelay, period, unit, scheduler);
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
    default <U, V> Observable<V> aggregateStart(
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
     * Aggregates the the sequence of this observable with the given
     * aggregator function and returns the last aggregated value.
     * @param <U>
     * @param seed
     * @param aggregator
     * @return
     */
    default <U> Observable<U> aggregateStart(U seed,
            BiFunction<? super U, ? super T, ? extends U> aggregator) {
        return aggregateStart(seed, aggregator, (i, v) -> v);
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
     * Aggregate this observable sequence with the given aggregator function
     * and finish up with the given divider function.
     * <p>If this observable is empty, the resulting observable is also
     * empty. If this observable contains only one element, that
     * is returned only.</p>
     * @param <U>
     * @param aggregator
     * @param divider
     * @return
     */
    default <U> Observable<U> aggregate(
            BiFunction<? super T, ? super T, ? extends T> aggregator,
            IndexedFunction<? super T, ? extends U> divider) {
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
                            U r = divider.apply(count, accumulator);
                            observer.next(r);
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
        return ObservableImpl.ambiguous(sources);
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
                                (v) -> {
                                    ls.sync(() -> {
                                        creg.remove(sreg);
                                        groups.remove(key).finish();
                                    });
                                },
                                (e) -> innerError(e),
                                () -> {
                                    ls.sync(() -> {
                                        creg.remove(sreg);
                                        groups.remove(key).finish();
                                    });
                                }
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
        return ObservableImpl.join(left, right, leftDuration, rightDuration, resultSelector);
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
        return ObservableImpl.repeat(value, count);
    }
    /**
     * Repeatedly registers with this observable if finishes normally
     * and the condition holds.
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
                        sreg.set(registerSafe(this));
                    } else {
                        observer.finish();
                        sreg.close();
                    }
                }
            };
            
            sreg.set(registerSafe(tobs));
            
            return sreg;
        };
    }
    /**
     * Returns an observable sequence which registers
     * with all of the source observables even if an error
     * event occurs
     * @param <T> the value type
     * @param sources
     * @return
     */
    public static <T> Observable<T> resumeAlways(Iterable<? extends Observable<? extends T>> sources) {
        return ObservableImpl.resumeAlways(sources);
    }
    /**
     * Returns an observable sequence which registers
     * with all of the source observables even if an error
     * event occurs
     * @param <T> the value type
     * @param sources
     * @return
     */
    public static <T> Observable<T> resumeOnError(Iterable<? extends Observable<? extends T>> sources) {
        return ObservableImpl.resumeOnError(sources);
    }
    /**
     * Creates an observable which periodically emits
     * timer values regardless of the registered observers
     * (like a wall clock).
     * @param start
     * @param count
     * @param period
     * @param unit
     * @param scheduler
     * @return
     */
    public static ObservableRegistration<Long> activeTick(long start, long count, long period,
            TimeUnit unit, Scheduler scheduler) {
        return ObservableImpl.activeTick(start, count, period, unit, scheduler);
    }
    /**
     * Adds a millisecond-based timestamp to each observed value.
     * @return
     */
    default Observable<Timestamped<T>> addTimestamped() {
        return select(v -> Timestamped.of(v));
    }
    /**
     * Records the millisecond-difference between subsequent elements.
     * @return
     */
    default Observable<TimeInterval<T>> addTimeInterval() {
        LongRef last = LongRef.of(System.currentTimeMillis());
        return select(v -> {
            long now = System.currentTimeMillis();
            long diff = now - last.value;
            last.value = now;
            return TimeInterval.of(v, diff);
        });
    }
    /**
     * Records the nanosecond-difference between subsequent elements.
     * @return
     */
    default Observable<TimeInterval<T>> addTimeIntervalNanos() {
        LongRef last = LongRef.of(System.nanoTime());
        return select(v -> {
            long now = System.nanoTime();
            long diff = now - last.value;
            last.value = now;
            return TimeInterval.of(v, diff);
        });
    }
    /**
     * Strips the {@link TimeInterval} wrapper from the source observable.
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Observable<T> removeTimeInterval(
            Observable<TimeInterval<? extends T>> source) {
        return source.select(v -> v.value());
    }
    /**
     * Strips the {@link Timestamped} wrapper from the source observable.
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Observable<T> removeTimestamped(
            Observable<Timestamped<? extends T>> source) {
        return source.select(v -> v.value());
    }
    /**
     * Computes the average of an integer stream.
     * @param source
     * @return
     */
    public static Observable<Integer> averageInteger(
            Observable<Integer> source) {
        return source.aggregate(
                (Integer v, Integer u) -> v + u,
                (int i, Integer v) -> v / i);
    }
    /**
     * Computes the average of a long stream.
     * @param source
     * @return
     */
    public static Observable<Long> averageLong(
            Observable<Long> source) {
        return source.aggregate(
                (Long v, Long u) -> v + u,
                (int i, Long v) -> v / i);
    }
    /**
     * Downcasts a wildcard typed source observable to
     * its base type.
     * <p>Sometimes this is necessary as the type inference
     * doesn't like U super (? extends T) like constructs.</p>
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Observable<T> downcast(Observable<? extends T> source) {
        return source.select(Function.identity());
    }
    /**
     * Average a sequence of numbers into a single double value.
     * @param source
     * @return
     */
    public static Observable<Double> averageNumber(
            Observable<? extends Number> source) {
        Observable<Number> src = downcast(source);
        return src.aggregate((u, v) -> u.doubleValue() + v.doubleValue(),
                (i, v) -> v.doubleValue() / i);
    }
    /**
     * Starts buffering after every {@code skip} number of elements
     * and buffers up to {@code bufferSize} element in each buffer.
     * @param bufferSize
     * @param skip
     * @return
     */
    default Observable<List<T>> buffer(int bufferSize, int skip) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("required bufferSize > 0");
        }
        if (skip <= 0) {
            throw new IllegalArgumentException("required skip > 0");
        }
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                /** The active buffers. */
                List<List<T>> buffers = new LinkedList<>();
                /** The element counter. */
                int i;
                @Override
                protected void onNext(T value) {
                    if (i % skip == 0) {
                        buffers.add(new ArrayList<>(bufferSize));
                    }
                    
                    Iterator<List<T>> it = buffers.iterator();
                    while (it.hasNext()) {
                        List<T> e = it.next();
                        e.add(value);
                        if (e.size() == bufferSize) {
                            it.remove();
                            try {
                                observer.next(e);
                            } catch (Throwable t) {
                                error(t);
                                return;
                            }
                        }
                    }
                    i++;
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                protected void onFinish() {
                    try {
                        List<List<T>> list = new ArrayList<>(buffers);
                        buffers.clear();
                        list.forEach(observer::next);
                        observer.finish();
                    } catch (Throwable t) {
                        observer.error(t);
                    }
                }
                
            };
            
            return register(tobs);
        };
    }
    /**
     * Buffers the elements of this observable into lists whose
     * boundary is determined by the arrival of the elements of
     * the {@code boundary} observable.
     * @param <U>
     * @param boundary
     * @return
     */
    default <U> Observable<List<T>> buffer(Observable<U> boundary) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            Lock lock = new ReentrantLock();
            
            BoolRef done = new BoolRef();
            Ref<List<T>> buffer = Ref.of(new ArrayList<>());
            
            Runnable onDone = () -> {
                if (done.value) {
                    return;
                }
                done.value = true;
                
                List<T> list = buffer.remove();
                observer.next(list);
                
                observer.finish();
            };
            Consumer<Throwable> onError = e -> {
                if (done.value) {
                    return;
                }
                done.value = true;
                observer.error(e);
            };
            
            DefaultObserver<T> tobs = new DefaultObserver<T>(creg, lock) {
                @Override
                protected void onNext(T value) {
                    if (done.value) {
                        return;
                    }
                    buffer.value.add(value);
                }
                @Override
                protected void onError(Throwable t) {
                    onError.accept(t);
                }
                @Override
                protected void onFinish() {
                    onDone.run();
                }
            };
            DefaultObserver<U> uobs = new DefaultObserver<U>(creg, lock) {
                @Override
                protected void onNext(U value) {
                    if (done.value) {
                        return;
                    }
                    List<T> list = buffer.replace(new ArrayList<>());
                    observer.next(list);
                }
                @Override
                protected void onError(Throwable t) {
                    onError.accept(t);
                }
                @Override
                protected void onFinish() {
                    onDone.run();
                }
            };
            
            creg.add(register(tobs));
            creg.add(boundary.register(uobs));
            
            return creg;
        };
    }
    /**
     * Concatenates the observable sequences produced by a selector
     * function for the specified parameter values.
     * @param <T>
     * @param <U>
     * @param selector
     * @param values
     * @return
     */
    @SafeVarargs
    public static <T, U> Observable<U> concat(
            Function<? super T, ? extends Observable<? extends U>> selector,
            T... values) {
        return concat(Arrays.asList(values), selector);
    }
    /**
     * Concatenates the observable sequences produced by a selector
     * function for the specified parameter values.
     * @param <T>
     * @param <U>
     * @param selector
     * @param values
     * @return
     */
    @SafeVarargs
    public static <T, U> Observable<U> concat(
            IndexedFunction<? super T, ? extends Observable<? extends U>> selector,
            T... values) {
        return concat(Arrays.asList(values), selector);
    }
    /**
     * Concatenates the observable sequences produced by a selector
     * function for the specified parameter values.
     * @param <T>
     * @param <U>
     * @param values
     * @param selector
     * @return
     */
    public static <T, U> Observable<U> concat(
            Iterable<? extends T> values,
            Function<? super T, ? extends Observable<? extends U>> selector) {
        return ObservableImpl.concat(values, selector);
    }
    /**
     * Concatenates the observable sequences produced by a selector
     * function for the specified parameter values.
     * @param <T>
     * @param <U>
     * @param values
     * @param selector
     * @return
     */
    public static <T, U> Observable<U> concat(
            Iterable<? extends T> values,
            IndexedFunction<? super T, ? extends Observable<? extends U>> selector) {
        return ObservableImpl.concat(values, selector);
    }
    /**
     * Returns an observable which contains a single value
     * if any of the values of this observable is equal to the
     * supplied value
     * @param value
     * @return
     */
    default Observable<Boolean> contains(T value) {
        return any(v -> Objects.equals(v, value));
    }
    /**
     * Returns the given value if this observable is empty.
     * @param defaultValue
     * @return
     */
    default Observable<T> ifEmpty(T defaultValue) {
        return ifEmpty(() -> defaultValue);
    }
    /**
     * Returns the value supplied by the function
     * if this observable is empty.
     * @param supplier
     * @return
     */
    default Observable<T> ifEmpty(Supplier<? extends T> supplier) {
        return (observer) -> {
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                boolean has;
                @Override
                public void onNext(T value) {
                    has = true;
                    observer.next(value);
                }
                @Override
                public void onError(Throwable t) {
                    observer.error(t);
                }
                @Override
                public void onFinish() {
                    if (!has) {
                        try {
                            observer.next(supplier.get());
                        } catch (Throwable t) {
                            error(t);
                            return;
                        }
                    }
                    observer.finish();
                }
                
            };
            return tobs.registerWith(this);
        };
    }
    /**
     * When an observer tries to register, it is registered
     * with the observable returned by the factory function.
     * @param <T>
     * @param factory
     * @return
     */
    public static <T> Observable<T> defer(Supplier<? extends Observable<? extends T>> factory) {
        return (observer) -> {
            try {
                return factory.get().registerSafe(observer);
            } catch (Throwable t) {
                observer.error(t);
                return Registration.EMPTY;
            }
        };
    }
    /**
     * Delays the registration and delivery of the events of this observable to
     * when the observable returned by the delaySelector fires
     * a value or finishes.
     *
     * @param <U>
     * @param registerDelay
     * @param delaySelector
     * @return
     */
    default <U, V> Observable<T> delay(
            Observable<U> registerDelay,
            Function<? super T, ? extends Observable<V>> delaySelector) {
        Objects.requireNonNull(registerDelay);
        return ObservableImpl.delay(this, registerDelay, delaySelector);
    }
    /**
     * Delays the delivery of the events of this observable to
     * when the observable returned by the delaySelector fires
     * a value or finishes.
     *
     * @param <U>
     * @param delaySelector
     * @return
     */
    default <U, V> Observable<T> delay(
            Function<? super T, ? extends Observable<V>> delaySelector) {
        return ObservableImpl.delay(this, null, delaySelector);
    }
    /**
     * Delays the registration on this observable to when
     * the register delay fires a value or finishes.
     * @param <U>
     * @param registerDelay
     * @return
     */
    default <U> Observable<T> delay(
            Observable<U> registerDelay) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            
            sreg.set(registerDelay.registerSafe(
                    new Observer<U>() {
                        boolean once;
                        @Override
                        public void next(U value) {
                            finish();
                        }
                        @Override
                        public void error(Throwable t) {
                            if (!once) {
                                once = true;
                                observer.error(t);
                                sreg.close();
                            }
                        }
                        @Override
                        public void finish() {
                            if (!once) {
                                once = true;
                                sreg.set(registerSafe(observer));
                            }
                        }
                    }.toThreadSafe()
            ));
            
            return sreg;
        };
    }
    /**
     * Dematerializes a notification stream into regular
     * event method calls.
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Observable<T> dematerialize(
            Observable<Notification<? extends T>> source) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            DefaultObserver<Notification<? extends T>> tobs = new DefaultObserver<Notification<? extends T>>(sreg) {
                @Override
                protected void onNext(Notification<? extends T> value) {
                    value.apply(observer);
                    if (value.isEmpty() || value.isException()) {
                        done();
                        close();
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
            sreg.set(source.registerSafe(tobs));
            return sreg;
        };
    }
    /**
     * Converts this observable's events into notification instances.
     * @return
     */
    default Observable<Notification<T>> materialize() {
        return (observer) -> {
            return register(Observer.create(
                    (v) -> observer.next(new Notification<>(v)),
                    (e) -> observer.next(new Notification<>(e)),
                    () -> observer.next(new Notification<>())
            ));
        };
    }
    /**
     * Returns an observable which contains the
     * given element at the given index or
     * remains empty if there aren't enough element in
     * this observable.
     * @param index
     * @return
     */
    default Observable<T> elementAt(int index) {
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            DefaultObserver<T> tobs = new DefaultObserver<T>(sreg) {
                int i;
                @Override
                protected void onNext(T value) {
                    if (i == index) {
                        observer.next(value);
                        finish();
                    }
                    i++;
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
            sreg.set(registerSafe(tobs));
            return sreg;
        };
    }
    /**
     * Runs the observables in parallel and combines their last
     * values when all finish.
     * @param <T>
     * @param sources
     * @return 
     */
    public static <T> Observable<List<T>> forkJoin(
            Iterable<? extends Observable<? extends T>> sources) {
        return ObservableImpl.forkJoin(sources);
    }
    /**
     * Runs the observables in parallel and combines their
     * last values when all finish.
     * @param <T>
     * @param sources
     * @return 
     */
    @SafeVarargs
    public static <T> Observable<List<T>> forkJoin(
            Observable<? extends T>... sources) {
        return forkJoin(Arrays.asList(sources));
    }
    /**
     * Creates an observable which tracks the result of the given
     * completable future.
     * <p>Cancelled futures appear as empty observables.</p>
     * @param <T>
     * @param cfuture
     * @return 
     */
    public static <T> Observable<T> create(CompletableFuture<? extends T> cfuture) {
        AsyncSubject<T> as = new AsyncSubject<>();
        
        cfuture.whenComplete((v, e) -> {
            boolean c = cfuture.isCancelled();
            if (e != null && !c) {
                as.error(e);
            } else {
                if (!c) {
                    as.next(v);
                }
                as.finish();
            }
        });
        
        return as;
    }
    /**
     * Creates an observable which awaits the completion of the
     * future on a scheduler.
     * <p>Interrupted or cancelled tasks appear as empty.</p>
     * @param <T>
     * @param future
     * @param scheduler
     * @return 
     */
    public static <T> Observable<T> create(Future<? extends T> future, Scheduler scheduler) {
        AsyncSubject<T> as = new AsyncSubject<>();
        
        scheduler.schedule(() -> {
            try {
                as.next(future.get());
                as.finish();
            } catch (InterruptedException | CancellationException ex) {
                as.finish();
            } catch (ExecutionException ex) {
                as.error(ex);
            }
        });
        
        return as;
    }
    /**
     * Returns an observable which generates values like a generalized
     * for loop.
     * @param <T>
     * @param start
     * @param test
     * @param next
     * @return 
     */
    public static <T> Observable<T> generate(T start, Predicate<? super T> test, Function<? super T, ? extends T> next) {
        return (observer) -> {
            try {
                T value = start;
                while (test.test(value)) {
                    observer.next(value);
                    value = next.apply(value);
                }
                observer.finish();
            } catch (Throwable t) {
                observer.error(t);
            }
            return Registration.EMPTY;
        };
    }
    /**
     * Registers a simple consumer and ignores error or finish events.
     * @param consumer
     * @return 
     */
    default Registration register(Consumer<? super T> consumer) {
        return registerSafe(new Observer<T>() {
            @Override
            public void next(T value) {
                consumer.accept(value);
            }
            @Override
            public void error(Throwable t) {
            }
            @Override
            public void finish() {
            }
        });
    }
    /**
     * Ignores the values of this observable.
     * @return 
     */
    default Observable<T> ignoreValues() {
        return (observer) -> 
            SimpleObserver.builder()
                .error(observer::error)
                .finish(observer::finish)
                .createAndRegister(this)
        ;
    }
}
