/*
 * Copyright 2011-2013 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.akarnokd.reactive4java.reactive;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import javax.annotation.Nonnull;

import hu.akarnokd.reactive4java.base.*;
import hu.akarnokd.reactive4java.util.*;

/**
 * Contains implementation classes for concatenating
 * various sources. See its inner classes.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Concat {
    /** Helper class. */
    private Concat() { }
    /**
     * Iterable main source.
     * @author akarnokd, 2013.01.13.
     */
    public static final class FromIterable {
        /** Helper class. */
        private FromIterable() { }
        /**
         * Concatenates the observable sequences resulting from enumerating
         * the sorce iterable and calling the resultSelector function.
         * <p>Remark: RX calls this For.</p>
         * @param <T> the source element type
         * @param <U> the result type
         */
        public static class Selector<T, U> implements Observable<U> {
            /** The source sequence. */
            @Nonnull
            protected final Iterable<? extends T> source;
            /** The result selector. */
            @Nonnull
            protected final Func1<? super T, ? extends Observable<? extends U>> resultSelector;
            /**
             * Constructor.
             * @param source the source sequence
             * @param resultSelector the observable selector
             */
            public Selector(
                    @Nonnull Iterable<? extends T> source, 
                    @Nonnull Func1<? super T, ? extends Observable<? extends U>> resultSelector) {
                        this.source = source;
                        this.resultSelector = resultSelector;
            }
            @Override
            @Nonnull
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                ConcatCoordinator cc = new ConcatCoordinator(observer, source.iterator());
                cc.nextSource();
                return cc;
            }
            
            final class ConcatCoordinator extends AtomicInteger implements Closeable {
                private static final long serialVersionUID = 8191337311454897333L;

                final Observer<? super U> actual;

                final Iterator<? extends T> it;
                
                final CompositeCloseable cc;
                
                volatile boolean active;
                
                ConcatCoordinator(Observer<? super U> actual, Iterator<? extends T> it) {
                    this.actual = actual;
                    this.it = it;
                    this.cc = new CompositeCloseable();
                    if (it instanceof Closeable) {
                        cc.add((Closeable)it);
                    }
                }
                
                void nextSource() {
                    active = false;
                    if (getAndIncrement() == 0) {
                        do {
                            if (!active) {
                                if (it.hasNext()) {
                                    T v = it.next();
                                    
                                    Observable<? extends U> o = resultSelector.invoke(v);
                                    Inner inner = new Inner();
                                    cc.add(inner);
                                    active = true;
                                    o.register(inner);
                                } else {
                                    actual.finish();
                                    return;
                                }
                            }
                        } while (decrementAndGet() != 0);
                    }
                }
                
                @Override
                public void close() throws IOException {
                    cc.close();
                }
                
                final class Inner extends AtomicReference<Closeable> implements Observer<U>, Closeable {

                    private static final long serialVersionUID = -8839940547797423702L;

                    @Override
                    public void error(Throwable ex) {
                        actual.error(ex);
                    }

                    @Override
                    public void finish() {
                        cc.delete(this);
                        nextSource();
                    }

                    @Override
                    public void next(U value) {
                        actual.next(value);
                    }
                    
                    @Override
                    public void close() throws IOException {
                        Closeable c = getAndSet(this);
                        if (c != null && c != this) {
                            c.close();
                        }
                    }
                    
                    void setCloseable(Closeable c) {
                        if (!compareAndSet(null, c)) {
                            Closeables.closeSilently(c);
                        }
                    }
                }
            }
        }
        /**
         * Concatenates the observable sequences resulting from enumerating
         * the sorce iterable and calling the indexed resultSelector function.
         * <p>Remark: RX calls this For.</p>
         * @param <T> the source element type
         * @param <U> the result type
         */
        public static class IndexedSelector<T, U> implements Observable<U> {
            /** The source sequence. */
            @Nonnull
            protected final Iterable<? extends T> source;
            /** The result selector. */
            @Nonnull
            protected final Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector;
            /**
             * Constructor.
             * @param source the source sequence
             * @param resultSelector the observable selector
             */
            public IndexedSelector(
                    @Nonnull Iterable<? extends T> source, 
                    @Nonnull Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector) {
                        this.source = source;
                        this.resultSelector = resultSelector;
            }
            @Override
            @Nonnull
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                ConcatCoordinator cc = new ConcatCoordinator(observer, source.iterator());
                cc.nextSource();
                return cc;
            }
            
            final class ConcatCoordinator extends AtomicInteger implements Closeable {
                private static final long serialVersionUID = 8191337311454897333L;

                final Observer<? super U> actual;

                final Iterator<? extends T> it;
                
                final CompositeCloseable cc;
                
                volatile boolean active;
                
                int index;
                
                ConcatCoordinator(Observer<? super U> actual, Iterator<? extends T> it) {
                    this.actual = actual;
                    this.it = it;
                    this.index = 1;
                    this.cc = new CompositeCloseable();
                    if (it instanceof Closeable) {
                        cc.add((Closeable)it);
                    }
                }
                
                void nextSource() {
                    active = false;
                    if (getAndIncrement() == 0) {
                        do {
                            if (!active) {
                                if (it.hasNext()) {
                                    T v = it.next();
                                    
                                    Observable<? extends U> o = resultSelector.invoke(v, index++);
                                    Inner inner = new Inner();
                                    cc.add(inner);
                                    active = true;
                                    inner.setCloseable(o.register(inner));
                                } else {
                                    actual.finish();
                                    return;
                                }
                            }
                        } while (decrementAndGet() != 0);
                    }
                }
                
                @Override
                public void close() throws IOException {
                    cc.close();
                }
                
                final class Inner extends AtomicReference<Closeable> implements Observer<U>, Closeable {

                    private static final long serialVersionUID = -8839940547797423702L;

                    @Override
                    public void error(Throwable ex) {
                        actual.error(ex);
                    }

                    @Override
                    public void finish() {
                        cc.delete(this);
                        nextSource();
                    }

                    @Override
                    public void next(U value) {
                        actual.next(value);
                    }
                    
                    @Override
                    public void close() throws IOException {
                        Closeable c = getAndSet(this);
                        if (c != null && c != this) {
                            c.close();
                        }
                    }
                    
                    void setCloseable(Closeable c) {
                        if (!compareAndSet(null, c)) {
                            Closeables.closeSilently(c);
                        }
                    }
                }
            }
        }
    }
    /**
     * Observable main source.
     * @author akarnokd, 2013.01.13.
     */
    public static final class FromObservable {
        /** Helper class. */
        private FromObservable() { }
        /**
         * Concatenate the the multiple sources selected by the function after another.
         * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
         * error, the outer observable will signal that error and the sequence is terminated.</p>
         * @author akarnokd, 2013.01.13.
         * @param <T> the source observable element type
         * @param <U> the result observable element type
         */
        public static class Selector<T, U> implements Observable<U> {
            /** The source. */
            protected final Observable<? extends Observable<? extends T>> sources;
            /** The result selector. */
            @Nonnull
            protected final Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> resultSelector;

            /**
             * Constructor.
             * @param sources the sources
             * @param resultSelector the concatenation result selector
             */
            public Selector(
                    Observable<? extends Observable<? extends T>> sources, 
                    Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> resultSelector) {
                this.sources = sources;
                this.resultSelector = resultSelector;
            }

            @Override
            @Nonnull 
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                ConcatCoordinator cc = new ConcatCoordinator(observer);
                cc.cc.add(sources.register(cc));
                return cc;
            }
            
            final class ConcatCoordinator extends AtomicInteger implements Observer<Observable<? extends T>>, Closeable {
                private static final long serialVersionUID = 3501088403564290965L;

                final Observer<? super U> actual;
                
                final CompositeCloseable cc;
                
                final ConcurrentLinkedQueue<Observable<? extends T>> queue;
                
                volatile boolean active;
                boolean mainFailure;
                volatile boolean done;
                
                ConcatCoordinator(Observer<? super U> actual) {
                    this.actual = actual;
                    this.cc = new CompositeCloseable();
                    this.queue = new ConcurrentLinkedQueue<Observable<? extends T>>();
                }

                @Override
                public void error(Throwable ex) {
                    cc.closeSilently();
                    mainFailure = true;
                    done = true;
                    synchronized (this) {
                        actual.error(ex);
                    }
                }
                
                void innerNext(U u) {
                    if (done && mainFailure) {
                        return;
                    }
                    synchronized (this) {
                        if (done && mainFailure) {
                            return;
                        }
                        actual.next(u);
                    }
                }

                @Override
                public void finish() {
                    done = true;
                    drain();
                }

                @Override
                public void close() throws IOException {
                    cc.close();
                }

                @Override
                public void next(Observable<? extends T> value) {
                    queue.offer(value);
                    drain();
                }
                
                void innerComplete() {
                    active = false;
                    drain();
                }
                
                void innerError(Throwable ex) {
                    cc.closeSilently();
                    if (done && mainFailure) {
                        return;
                    }
                    synchronized (this) {
                        if (done && mainFailure) {
                            return;
                        }
                        actual.error(ex);
                    }
                }
                
                void drain() {
                    if (getAndIncrement() == 0) {
                        do {
                            if (!active) {
                                boolean d = done;
                                boolean err = mainFailure;
                                Observable<? extends T> o = queue.poll();
                                boolean empty = o == null;

                                if (d && err) {
                                    return;
                                }
                                
                                if (d && empty) {
                                    actual.finish();
                                    return;
                                }
                                
                                if (!empty) {
                                    Observable<? extends U> p = resultSelector.invoke(o);
                                    Inner inner = new Inner();
                                    cc.add(inner);
                                    active = true;
                                    inner.setCloseable(p.register(inner));
                                }
                            }
                        } while (decrementAndGet() != 0);
                    }
                }
                final class Inner extends AtomicReference<Closeable> implements Observer<U>, Closeable {

                    private static final long serialVersionUID = -8839940547797423702L;

                    @Override
                    public void error(Throwable ex) {
                        innerError(ex);
                    }

                    @Override
                    public void finish() {
                        cc.delete(this);
                        innerComplete();
                    }

                    @Override
                    public void next(U value) {
                        innerNext(value);
                    }
                    
                    @Override
                    public void close() throws IOException {
                        Closeable c = getAndSet(this);
                        if (c != null && c != this) {
                            c.close();
                        }
                    }
                    
                    void setCloseable(Closeable c) {
                        if (!compareAndSet(null, c)) {
                            Closeables.closeSilently(c);
                        }
                    }
                }
            }
        }
        /**
         * Concatenate the the multiple sources selected by the indexed function after another.
         * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
         * error, the outer observable will signal that error and the sequence is terminated.</p>
         * @author akarnokd, 2013.01.13.
         * @param <T> the source observable element type
         * @param <U> the result observable element type
         */
        public static class IndexedSelector<T, U> implements Observable<U> {
            /** The source. */
            @Nonnull
            protected final Observable<? extends Observable<? extends T>> sources;
            /** The result selector. */
            @Nonnull
            protected final Func2<? super Observable<? extends T>, ? super Integer, ? extends Observable<? extends U>> resultSelector;

            /**
             * Constructor.
             * @param sources the sources
             * @param resultSelector the indexed concatenation result selector
             */
            public IndexedSelector(
                    Observable<? extends Observable<? extends T>> sources, 
                    Func2<? super Observable<? extends T>, ? super Integer, ? extends Observable<? extends U>> resultSelector) {
                this.sources = sources;
                this.resultSelector = resultSelector;
            }

            @Override
            @Nonnull 
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                ConcatCoordinator cc = new ConcatCoordinator(observer);
                cc.cc.add(sources.register(cc));
                return cc;
            }
            
            final class ConcatCoordinator extends AtomicInteger implements Observer<Observable<? extends T>>, Closeable {
                private static final long serialVersionUID = 3501088403564290965L;

                final Observer<? super U> actual;
                
                final CompositeCloseable cc;
                
                final ConcurrentLinkedQueue<Observable<? extends T>> queue;
                
                volatile boolean active;
                boolean mainFailure;
                volatile boolean done;

                int index;
                
                ConcatCoordinator(Observer<? super U> actual) {
                    this.actual = actual;
                    this.cc = new CompositeCloseable();
                    this.index = 1;
                    this.queue = new ConcurrentLinkedQueue<Observable<? extends T>>();
                }

                @Override
                public void error(Throwable ex) {
                    cc.closeSilently();
                    mainFailure = true;
                    done = true;
                    synchronized (this) {
                        actual.error(ex);
                    }
                }
                
                void innerNext(U u) {
                    if (done && mainFailure) {
                        return;
                    }
                    synchronized (this) {
                        if (done && mainFailure) {
                            return;
                        }
                        actual.next(u);
                    }
                }

                @Override
                public void finish() {
                    done = true;
                    drain();
                }

                @Override
                public void close() throws IOException {
                    cc.close();
                }

                @Override
                public void next(Observable<? extends T> value) {
                    queue.offer(value);
                    drain();
                }
                
                void innerComplete() {
                    active = false;
                    drain();
                }
                
                void innerError(Throwable ex) {
                    cc.closeSilently();
                    if (done && mainFailure) {
                        return;
                    }
                    synchronized (this) {
                        if (done && mainFailure) {
                            return;
                        }
                        actual.error(ex);
                    }
                }
                
                void drain() {
                    if (getAndIncrement() == 0) {
                        do {
                            if (!active) {
                                boolean d = done;
                                boolean err = mainFailure;
                                Observable<? extends T> o = queue.poll();
                                boolean empty = o == null;

                                if (d && err) {
                                    return;
                                }
                                
                                if (d && empty) {
                                    actual.finish();
                                    return;
                                }
                                
                                if (!empty) {
                                    Observable<? extends U> p = resultSelector.invoke(o, index++);
                                    Inner inner = new Inner();
                                    cc.add(inner);
                                    active = true;
                                    inner.setCloseable(p.register(inner));
                                }
                            }
                        } while (decrementAndGet() != 0);
                    }
                }
                final class Inner extends AtomicReference<Closeable> implements Observer<U>, Closeable {

                    private static final long serialVersionUID = -8839940547797423702L;

                    @Override
                    public void error(Throwable ex) {
                        innerError(ex);
                    }

                    @Override
                    public void finish() {
                        cc.delete(this);
                        innerComplete();
                    }

                    @Override
                    public void next(U value) {
                        innerNext(value);
                    }
                    
                    @Override
                    public void close() throws IOException {
                        Closeable c = getAndSet(this);
                        if (c != null && c != this) {
                            c.close();
                        }
                    }
                    
                    void setCloseable(Closeable c) {
                        if (!compareAndSet(null, c)) {
                            Closeables.closeSilently(c);
                        }
                    }
                }
            }
        }
    }
}
