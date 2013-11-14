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

package hu.akarnokd.reactive4java8.base;

import static hu.akarnokd.reactive4java8.base.Observable.empty;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of various static observable operators
 * to reduce the length of the Observable interface.
 * @author karnok
 */
final class ObservableOps {
    /** No instances. */
    private ObservableOps() { throw new UnsupportedOperationException(); }
    /**
     * Returns an observable sequence which registers
     * with all of the source observables even if an error
     * event occurs
     * @param sources
     * @return 
     */
    static <T> Observable<T> resumeAlways(Iterable<? extends Observable<? extends T>> sources) {
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
    static <T> Observable<T> resumeOnError(Iterable<? extends Observable<? extends T>> sources) {
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
    static <T> Observable<T> repeat(T value, int count) {
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
    static <L, R, LD, RD, V> Observable<V> join(
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
    static <T, U, V> Observable<V> combineLatest(
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
    static <T> Observable<T> ambiguous(
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
    static Observable<Long> tick(long start, long count, 
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
    static Observable<Long> tick(long start, long count, 
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
    static Observable<Long> tick(long period, TimeUnit unit, 
            Scheduler scheduler) {
        return (observer) -> {
            LongRef count = new LongRef();
            return scheduler.schedule(period, period, unit, () -> {
                observer.next(count.value++);
            });
        };
    }
}
