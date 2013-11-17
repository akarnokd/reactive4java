/*
 * Copyright 2013 karnok.
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

package hu.akarnokd.reactive4java8.observables;

import hu.akarnokd.reactive4java8.registrations.CompositeRegistration;
import hu.akarnokd.reactive4java8.registrations.SingleRegistration;
import hu.akarnokd.reactive4java8.Scheduler;
import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.Observer;
import hu.akarnokd.reactive4java8.Observable;
import hu.akarnokd.reactive4java8.observers.DefaultObserver;
import hu.akarnokd.reactive4java8.util.LockSync;
import hu.akarnokd.reactive4java8.util.IndexedFunction;
import hu.akarnokd.reactive4java8.util.LongRef;
import hu.akarnokd.reactive4java8.util.Ref;
import hu.akarnokd.reactive4java8.util.BoolRef;
import hu.akarnokd.reactive4java8.util.IntRef;
import static hu.akarnokd.reactive4java8.Observable.empty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Implementation of various static observable operators
 * to reduce the length of the Observable interface.
 * @author karnok
 */
public final class ObservableImpl {
    /** No instances. */
    private ObservableImpl() { throw new UnsupportedOperationException(); }
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
            
            Observer<T> so = Observer.create(
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
            
            Observer<Observable<? extends T>> sourceObserver = Observer.create(
                    (o) -> {
                        wip.incrementAndGet();
                        
                        SingleRegistration itemReg = new SingleRegistration();
                        
                        reg.add(itemReg);
                        
                        Observer<T> itemObserver = Observer.create(
                                observer::next,
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
        DefaultObservable<Long> obs = new DefaultObservable<>();
        
        LongRef counter = LongRef.of(start);
        long end = start + count;
        
        SingleRegistration sreg = new SingleRegistration();
        sreg.set(scheduler.schedule(period, period, unit, () -> {
           if (counter.value < end) {
               obs.next(counter.value);
               counter.value++;
           } else {
               sreg.close();
           }
        }));
        
        return new ObservableRegistration<Long>() {
            @Override
            public Registration register(Observer<? super Long> observer) {
                return obs.register(observer);
            }

            @Override
            public void close() {
                obs.clear();
                sreg.close();
            }
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
    public static <T, U> Observable<U> concat(
            Iterable<? extends T> values,
            Function<? super T, ? extends Observable<? extends U>> selector
            ) {
        Objects.requireNonNull(selector);
        Objects.requireNonNull(values);
        return (observer) -> {
            
            Iterator<? extends T> it = values.iterator();
            
            SingleRegistration sreg = new SingleRegistration();
            
            Observer<U> uobs = new Observer<U>() {
                @Override
                public void next(U value) {
                    observer.next(value);
                }
                @Override
                public void error(Throwable t) {
                    observer.error(t);
                    sreg.close();
                }
                @Override
                public void finish() {
                    if (it.hasNext()) {
                        try {
                            sreg.set(selector.apply(it.next()).register(this));
                        } catch (Throwable t) {
                            error(t);
                        }
                    } else {
                        observer.finish();
                        sreg.close();
                    }
                }
            }.toThreadSafe();
            
            uobs.finish();
            
            return sreg;
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
    public static <T, U> Observable<U> concat(
            Iterable<? extends T> values,
            IndexedFunction<? super T, ? extends Observable<? extends U>> selector 
            ) {
        Objects.requireNonNull(selector);
        Objects.requireNonNull(values);
        return (observer) -> {
            Iterator<? extends T> it = values.iterator();
            
            SingleRegistration sreg = new SingleRegistration();
            
            Observer<U> uobs = new Observer<U>() {
                /** The index counter. */
                int index;
                @Override
                public void next(U value) {
                    observer.next(value);
                }
                @Override
                public void error(Throwable t) {
                    observer.error(t);
                    sreg.close();
                }
                @Override
                public void finish() {
                    if (it.hasNext()) {
                        try {
                            sreg.set(selector.apply(index++, it.next()).register(this));
                        } catch (Throwable t) {
                            error(t);
                        }
                    } else {
                        observer.finish();
                        sreg.close();
                    }
                }
            }.toThreadSafe();
            
            uobs.finish();
            
            return sreg;
        };
    }
    /**
     * Delays the delivery of the events of this observable to
     * when the observable returned by the delaySelector fires
     * a value or finishes.
     * 
     * @param <T>
     * @param <U>
     * @param <V>
     * @param registerDelay
     * @param delaySelector
     * @return 
     */
    public static <T, U, V> Observable<T> delay(
            Observable<? extends T> source,
            @Nullable Observable<U> registerDelay,
            Function<? super T, ? extends Observable<V>> delaySelector) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(delaySelector);
        return (observer) -> {
            SingleRegistration sreg = new SingleRegistration();
            CompositeRegistration creg = new CompositeRegistration();
            SingleRegistration rreg = new SingleRegistration();
            
            DefaultObserver<T> tobs = new DefaultObserver<T>() {
                boolean atEnd;
                @Override
                protected void onNext(T value) {
                    Observable<V> delay;
                    try {
                        delay = delaySelector.apply(value);
                    } catch (Throwable t) {
                        error(t);
                        return;
                    }
                    SingleRegistration dreg = new SingleRegistration();
                    
                    creg.add(dreg);
                    
                    dreg.set(delay.registerSafe(new Observer<V>() {
                        @Override
                        public void next(V v0) {
                            deliver(dreg, value);
                        }
                        @Override
                        public void error(Throwable t) {
                            innerError(t);
                        }
                        @Override
                        public void finish() {
                            deliver(dreg, value);
                        }
                    }));
                }
                /**
                 * Deliver the given value and unregister the closeable.
                 * @param reg
                 * @param value 
                 */
                protected void deliver(Registration reg, T value) {
                    creg.close(reg);
                    try {
                        ls.sync(() -> {
                            if (!checkDone()) {
                                observer.next(value);
                            }
                            checkAtEnd();
                        });
                    } catch (Throwable t) {
                        error(t);
                    }
                }
                /** 
                 * Forward the internal error.
                 * @param t 
                 */
                protected void innerError(Throwable t) {
                    error(t);
                }
                @Override
                protected void onError(Throwable t) {
                    observer.error(t);
                    creg.close();
                    sreg.close();
                }
                @Override
                protected void onFinish() {
                    atEnd = true;
                    rreg.close();
                    checkAtEnd();
                }
                /** Check if the end is reached and signal a finish. */
                void checkAtEnd() {
                    if (atEnd && creg.isEmpty()) {
                        observer.finish();
                        sreg.close();
                    }
                }
            };
            
            if (registerDelay == null) {
                sreg.set(source.registerSafe(tobs));
            } else {
                rreg.set(registerDelay.registerSafe(new Observer<U>() {
                    boolean once;
                    @Override
                    public void next(U value) {
                        finish();
                    }
                    @Override
                    public void error(Throwable t) {
                        tobs.error(t);
                        rreg.close();
                    }
                    @Override
                    public void finish() {
                        if (!once) {
                            once = true;
                            rreg.close();
                            sreg.set(source.registerSafe(tobs));
                        }
                    }
                }.toThreadSafe()));
            }
            
            return new CompositeRegistration(rreg, sreg, creg);
        };
    }
    /**
     * Runs the observables in parallel and combines their last
     * values when all finishes.
     * @param <T>
     * @param sources
     * @return 
     */
    public static <T> Observable<List<T>> forkJoin(
            Iterable<? extends Observable<? extends T>> sources) {
        return (observer) -> {
            CompositeRegistration creg = new CompositeRegistration();
            
            AtomicInteger wip = new AtomicInteger(1);
            
            LockSync lsb = new LockSync();
            List<T> buffer = new ArrayList<>();
            
            Runnable onDone = () -> {
                if (wip.decrementAndGet() == 0) {
                    try {
                        observer.next(buffer);
                    } catch (Throwable t) {
                        observer.error(t);
                        return;
                    }
                    observer.finish();
                }
            };
            
            int i = 0;
            for (Observable<? extends T> o : sources) {
                SingleRegistration sreg = new SingleRegistration();
                creg.add(sreg);
                
                wip.incrementAndGet();
                lsb.sync(() -> buffer.add(null));
                
                int j = i;
                DefaultObserver<T> tobs = new DefaultObserver<T>(sreg) {
                    T last;
                    @Override
                    protected void onNext(T value) {
                        this.last = value;
                    }
                    @Override
                    protected void onError(Throwable t) {
                        observer.error(t);
                        creg.close();
                    }
                    @Override
                    protected void onFinish() {
                        lsb.sync(() -> {
                            buffer.set(j, last);
                        });
                        onDone.run();
                    }
                };
                
                sreg.set(o.registerSafe(tobs));
                i++;
            }
            
            onDone.run();
            
            return creg;
        };
    }
}
