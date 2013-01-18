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

import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.Subject;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.SingleCloseable;
import hu.akarnokd.reactive4java.util.Unique;

import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Helper class for the Reactive.window operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Windowing {
	/** Helper class. */
	private Windowing() { }
	/**
	 * Projects the parts of the source into non-overlapping observable
	 * sequences until the window observable finishes.
	 * If the window finishes, a new window is started along with a new
	 * observable in the output
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 */
	public static class WithClosing<T, U> implements Observable<Observable<T>> {
		/** The source observable. */
		@Nonnull
		protected final Observable<? extends T> source;
		/** The window closing selector for each registerer. */
		@Nonnull
		protected final Func0<? extends Observable<U>> windowClosingSelector;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param windowClosingSelector the window selector
		 */
		public WithClosing(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Func0<? extends Observable<U>> windowClosingSelector) {
			this.source = source;
			this.windowClosingSelector = windowClosingSelector;
			
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The current open window. */
				@GuardedBy("lock")
				Subject<T, T> window;

				@Override
				protected void onNext(T value) {
					window.next(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					window.error(ex);
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					window.finish();
					observer.finish();
				}
				/** Create a new observable window. */
				protected void newWindow() {
					lock.lock();
					try {
						window = new DefaultObservable<T>();
						observer.next(window);
					} finally {
						lock.unlock();
					}
				}
				/**
				 * Callout from the inner observable.
				 * @param ex the exception
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Call init from sub-observers. */
				protected void innerInit() {
					init();
				}
				@Override
				public void init() {
					newWindow();
					DefaultObserverEx<U> closeObserver = new DefaultObserverEx<U>(lock, true) {
						@Override
						protected void onNext(U value) {
							// ignored
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							window.finish();
							innerInit();
						}
						
					};
					
					add("windowClosing", closeObserver.registerWith(windowClosingSelector.invoke()));
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Projects the source elements into multiple, potentially overlapping
	 * windows. The window is opened by an element
	 * from the windowOpenings sequence and closed once the
	 * associated windowClosingSelector's value finishes (i.e., if it produces elements,
	 * they are ignored).
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window opening element type
	 * @param <V> the window closing element type.
	 */
	public static class WithOpenClose<T, U, V> implements Observable<Observable<T>> {
		/** */
		protected Observable<? extends T> source;
		/** */
		protected Observable<? extends U> windowOpenings;
		/** */
		protected Func1<? super U, ? extends Observable<V>> windowClosingSelector;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param windowOpenings the sequence producing the window-open events
		 * @param windowClosingSelector the selector producing a window-closing sequence for
		 * the pair window-open event.
		 */
		public WithOpenClose(
				@Nonnull Observable<? extends T> source,
				@Nonnull Observable<? extends U> windowOpenings,
				@Nonnull Func1<? super U, ? extends Observable<V>> windowClosingSelector
				) {
			this.source = source;
			this.windowOpenings = windowOpenings;
			this.windowClosingSelector = windowClosingSelector;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			/** The source observer instance. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The open windows map. */
				@GuardedBy("lock")
				Map<Unique<U>, Subject<T, T>> openMap = new HashMap<Unique<U>, Subject<T, T>>();
				
				@Override
				protected void onNext(T value) {
					for (Subject<T, T> s : openMap.values()) {
						s.next(value);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					for (Subject<T, T> s : openMap.values()) {
						s.error(ex);
					}
					observer.error(ex);
					openMap = new HashMap<Unique<U>, Subject<T, T>>();
				}
				/**
				 * Relay exceptions from inner observers.
				 * @param ex the exception
				 */
				protected void innerError(Throwable ex) {
					error(ex);
				}
				@Override
				protected void onFinish() {
					for (Subject<T, T> s : openMap.values()) {
						s.finish();
					}
					observer.finish();
					openMap = new HashMap<Unique<U>, Subject<T, T>>();
				}
				@Override
				public void init() {
					/** Observer for window open values. */
					class OpenObserver extends DefaultObserverEx<U> {
						/**
						 * Constructor.
						 * @param lock the shared lock
						 * @param terminate terminate on finish?
						 */
						public OpenObserver(Lock lock, boolean terminate) {
							super(lock, terminate);
						}
						@Override
						protected void onNext(U value) {
							final Unique<U> token = Unique.of(value);
							final Observable<V> cl = windowClosingSelector.invoke(value);
							
							final Subject<T, T> s = new DefaultObservable<T>();
							
							openMap.put(token, s);
							
							observer.next(s);
							/** The window finish observer. */
							class FinishObserver extends DefaultObserverEx<V> {
								/**
								 * Constructor.
								 * @param lock the shared lock
								 */
								public FinishObserver(Lock lock) {
									super(lock, true);
								}
								@Override
								protected void onNext(V value) {
									// ignored
								}

								@Override
								protected void onError(Throwable ex) {
									innerError(ex);
								}

								@Override
								protected void onFinish() {
									Subject<T, T> s = openMap.remove(token);
									s.finish();
								}
								
							}
							FinishObserver fo = new FinishObserver(lock);
							add(token, fo.registerWith(cl));
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							remove(this);
						}
						
					}
					add("windowOpening", new OpenObserver(lock, false).registerWith(windowOpenings));
				}
			}
			SourceObserver o = new SourceObserver();
			return o.registerWith(source);
		}
	}
	/**
	 * Projects the source elements into a non-overlapping consecutive windows.
	 * <p>The first window opens immediately, The current window is closed when 
	 * the boundary observable sequence has sent a value. The finish
	 * of the boundary will finish both inner and outer observables.
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window boundary element type (irrelevant
	 */
	public static class WithBoundary<T, U> implements Observable<Observable<T>> {
		/** The soruce sequence. */
		protected Observable<? extends T> source;
		/** The boundary sequence. */
		protected Observable<U> boundary;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param boundary the window boundary indicator.
		 */
		public WithBoundary(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Observable<U> boundary) {
			this.source = source;
			this.boundary = boundary;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			/** The source observer. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The current window. */
				@GuardedBy("lock")
				Subject<T, T> window;
				@Override
				protected void onNext(T value) {
					window.next(value);
				}

				@Override
				protected void onError(Throwable ex) {
					window.error(ex);
					observer.error(ex);
				}
				/** 
				 * Called from inner.
				 * @param ex the inner exception 
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Called from inner. */
				protected void innerFinish() {
					finish();
				}
				@Override
				protected void onFinish() {
					window.finish();
					observer.finish();
				}
				@Override
				protected void onRegister() {
					/** The boundary observer. */
					class BoundaryObserver extends DefaultObserverEx<U> {
						/**
						 * Constructor with shared lock.
						 * @param lock the shared lock
						 */
						public BoundaryObserver(Lock lock) {
							super(lock, true);
						}

						@Override
						protected void onNext(U value) {
							window.finish();
							window = new DefaultObservable<T>();
							observer.next(window);
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							innerFinish();
						}
					}
					lock.lock();
					try {
						window = new DefaultObservable<T>();
						observer.next(window);
					} finally {
						lock.unlock();
					}
					add("boundary", new BoundaryObserver(lock).registerWith(boundary));
				}
			}
			return (new SourceObserver()).registerWith(source);
		}
	}
	/**
	 * Project the source sequence to
	 * potentially overlapping windows whose
	 * start is determined by skip and lengths
	 * by size.
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the element type
	 */
	public static class WithSizeSkip<T> implements Observable<Observable<T>> {
		/** */
		protected final Observable<? extends T> source;
		/** */
		protected final int size;
		/** */
		protected final int skip;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param size the window size
		 * @param skip skip between windows
		 */
		public WithSizeSkip(Observable<? extends T> source, int size, int skip) {
			this.source = source;
			this.size = size;
			this.skip = skip;
			
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
				/** The queue of open windows. */
				@GuardedBy("lock")
				final Queue<Subject<T, T>> queue = new LinkedList<Subject<T, T>>();
				/** The current element index. */
				@GuardedBy("lock")
				int i;
				@Override
				protected void onNext(T value) {
					for (Subject<T, T> s : queue) {
						s.next(value);
					}
					int c = i - size + 1;
					if (c >= 0 && c % skip == 0) {
						queue.poll().finish();
					}
					
					i++;
					if (i % skip == 0) {
						Subject<T, T> s = new DefaultObservable<T>();
						queue.add(s);
						observer.next(s);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					while (!queue.isEmpty()) {
						queue.poll().error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					while (!queue.isEmpty()) {
						queue.poll().finish();
					}
					observer.finish();
				}
				@Override
				protected void onRegister() {
					Subject<T, T> s = new DefaultObservable<T>();
					queue.add(s);
					observer.next(s);
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Projects elements from the source observable
	 * into zero or more windows which are produced
	 * based on timing information.
	 * <p>The implementation ensures that if timespan
	 * and timeshift are equal, the windows won't overlap.</p>
	 * @author akarnokd, 2013.01.17.
	 * @param <T> the source element type
	 */
	public static class WithTime<T> implements Observable<Observable<T>> {
		/** */
		protected final Observable<? extends T> source;
		/** */
		protected final long timespan;
		/** */
		protected final long timeshift;
		/** */
		protected final TimeUnit unit;
		/** */
		protected final Scheduler pool;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param timespan the length of each window
		 * @param timeshift the interval between the creation of consequtive windows
		 * @param unit the time unit
		 * @param pool the scheduler for the timed oeprations
		 */
		public WithTime(
				Observable<? extends T> source,
				long timespan,
				long timeshift,
				TimeUnit unit,
				Scheduler pool
		) {
			this.source = source;
			this.timespan = timespan;
			this.timeshift = timeshift;
			this.unit = unit;
			this.pool = pool;
			
		}
		@Override
		@Nonnull
		public Closeable register(Observer<? super Observable<T>> observer) {
			if (timespan == timeshift) {
				return exactWindow(observer);
			}
			return generalWindow(observer);
		}
		/**
		 * Create windows by an exact timing.
		 * @param observer the observer
		 * @return the close handle
		 */
		protected Closeable exactWindow(final Observer<? super Observable<T>> observer) {
			final CompositeCloseable c = new CompositeCloseable();
			final SingleCloseable rc = new SingleCloseable();
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			/** The source observer. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The current window. */
				Subject<T, T> window;
				/**
				 * Constructor. 
				 * @param lock the lock to use
				 */
				public SourceObserver(Lock lock) {
					super(lock, true);
				}
				@Override
				protected void onNext(T value) {
					window.next(value);
				}

				@Override
				protected void onError(Throwable ex) {
					if (window != null) {
						window.error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					if (window != null) {
						window.finish();
					}
					observer.finish();
				}
				@Override
				protected void onRegister() {
					newWindow();
				}
				/** Create a new window. */
				@GuardedBy("lock")
				protected void newWindow() {
					if (window != null) {
						window.finish();
					}
					window = new DefaultObservable<T>();
					observer.next(window);
				}
			};
			
			final SourceObserver obs = new SourceObserver(lock);
			
			DefaultRunnable run = new DefaultRunnable(lock) {
				@Override
				protected void onRun() {
					obs.newWindow();
				}
			};
			
			c.add(obs, rc);

			obs.add("both", c);
			
			obs.registerWith(source);
			rc.set(pool.schedule(run, timespan, timespan, unit));
			
			return c;
		}
		/**
		 * Create a general windowing operation.
		 * @param observer the output observer
		 * @return the close handler
		 */
		protected Closeable generalWindow(final Observer<? super Observable<T>> observer) {
			final CompositeCloseable c = new CompositeCloseable();

			final SingleCloseable openTimer = new SingleCloseable();
			
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			/** The source observer. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The currently open windows. */
				public final Queue<Subject<T, T>> openWindows = new LinkedList<Subject<T, T>>();
				/**
				 * Constructor.
				 * @param lock the common lock
				 */
				public SourceObserver(Lock lock) {
					super(lock, true);
				}

				@Override
				protected void onNext(T value) {
					for (Subject<T, T> s : openWindows) {
						s.next(value);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					for (Subject<T, T> s : openWindows) {
						s.error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					for (Subject<T, T> s : openWindows) {
						s.finish();
					}
					observer.finish();
				}
				/** Opens a new window. */
				@GuardedBy("lock")
				protected void openWindow() {
					final Subject<T, T> s = new DefaultObservable<T>();
					openWindows.add(s);
					final Object token = new Object();
					add(token, pool.schedule(new DefaultRunnable(lock) { 
						@Override
						protected void onRun() {
							closeWindow(token, s);
						}
					}, timespan, unit));
					observer.next(s);
				}
				/** 
				 * Closes the given window and removes the token.
				 * @param token the token
				 * @param s the window
				 */
				protected void closeWindow(Object token, Subject<T, T> s) {
					subObservers.delete(token);
					openWindows.remove(s);
					s.finish();
				}
				@Override
				protected void onRegister() {
					openWindow();
				}
			}
			final SourceObserver obs = new SourceObserver(lock);
			
			DefaultRunnable openRun = new DefaultRunnable(lock) {
				@Override
				protected void onRun() {
					obs.openWindow();
				}
			};
			
			c.add(obs, openTimer);
			obs.add("both", c);
			
			obs.registerWith(source);
			openTimer.set(pool.schedule(openRun, timeshift, unit));
			
			return c;
		}
	}
	/**
	 * Projects each element into a window that
	 * is completed by either its full or the specified
	 * amount of time elapsed.
	 * Time periods are absolute from the beginning of
	 * the streaming.
	 * @author akarnokd, 2013.01.17.
	 * @param <T> the element type
	 */
	public static class WithTimeOrSize<T> implements Observable<Observable<T>> {
		/** */
		protected final Observable<? extends T> source;
		/** */
		protected final int size;
		/** */
		protected final long timespan;
		/** */
		protected final TimeUnit unit;
		/** */
		private Scheduler pool;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param size the window size
		 * @param timespan the window length
		 * @param unit the time unit
		 * @param pool the scheduler to run the timed operations
		 */
		public WithTimeOrSize(
				Observable<? extends T> source,
				int size,
				long timespan,
				TimeUnit unit,
				Scheduler pool
		) {
			this.source = source;
			this.size = size;
			this.timespan = timespan;
			this.unit = unit;
			this.pool = pool;
			
		}
		@Override
		@Nonnull
		public Closeable register(final Observer<? super Observable<T>> observer) {
			final CompositeCloseable c = new CompositeCloseable();
			final SingleCloseable rc = new SingleCloseable();
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			/** The source observer. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The current window. */
				Subject<T, T> window;
				/** The window count. */
				int count;
				/**
				 * Constructor. 
				 * @param lock the lock to use
				 */
				public SourceObserver(Lock lock) {
					super(lock, true);
				}
				@Override
				protected void onNext(T value) {
					window.next(value);
					if (++count == size) {
						newWindow();
					}
				}

				@Override
				protected void onError(Throwable ex) {
					if (window != null) {
						window.error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					if (window != null) {
						window.finish();
					}
					observer.finish();
				}
				@Override
				protected void onRegister() {
					newWindow();
				}
				/** Create a new window. */
				@GuardedBy("lock")
				protected void newWindow() {
					if (window != null) {
						window.finish();
					}
					window = new DefaultObservable<T>();
					observer.next(window);
					count = 0;
				}
			};
			
			final SourceObserver obs = new SourceObserver(lock);
			
			DefaultRunnable run = new DefaultRunnable(lock) {
				@Override
				protected void onRun() {
					obs.newWindow();
				}
			};
			
			c.add(obs, rc);

			obs.add("both", c);
			
			obs.registerWith(source);
			rc.set(pool.schedule(run, timespan, timespan, unit));
			
			return c;
		}
	}
	
}
