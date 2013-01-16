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
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.Unique;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Buffering related observable implementations.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Buffer {
	/** Helper class. */
	private Buffer() { }
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * The scheduling is done on the supplied Scheduler.
	 * @param <T> the type of elements to observe
	 * @return the observable of list of Ts
	 * @author akarnokd, 2013.01.13.
	 */
	public static final class WithTime<T> implements Observable<List<T>> {
		/** The source sequence. */
		private final Observable<? extends T> source;
		/** The wait unit. */
		private final TimeUnit unit;
		/** The wait time. */
		private final long time;
		/** The scheduler to wait on. */
		private final Scheduler pool;
		/**
		 * @param source the source of Ts.
		 * @param time the time value to split the buffer contents.
		 * @param unit the time unit of the time
		 * @param pool the scheduled execution pool to use
		 */
		public WithTime(
				Observable<? extends T> source, 
				long time,
				TimeUnit unit,
				Scheduler pool) {
			this.source = source;
			this.unit = unit;
			this.time = time;
			this.pool = pool;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {

			final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());

			final DefaultRunnable r = new DefaultRunnable(lock) {
				@Override
				public void onRun() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					observer.next(curr);
				}
			};
			DefaultObserverEx<T> o = new DefaultObserverEx<T>(lock, true) {
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				public void onFinish() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					observer.next(curr);
					observer.finish();
				}
				/** The buffer to fill in. */
				@Override
				public void onNext(T value) {
					buffer.add(value);
				}
				@Override
				public void init() {
					add("timer", pool.schedule(r, time, time, unit));
				}
			};
			return o.registerWith(source);
		}
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the buffer runs out.
	 * @param <T> the type of the values
	 * @return the observable of list of Ts
	 * @author akarnokd, 2013.01.13.
	 */
	public static final class WithSizeOrTime<T> implements Observable<List<T>> {
		/** The wait time unit. */
		private final TimeUnit unit;
		/** The wait time. */
		private final long time;
		/** The pool for the waiting. */
		private final Scheduler pool;
		/** The source sequence. */
		private final Observable<? extends T> source;
		/** The allowed maximum buffer size. */
		private final int bufferSize;

		/**
		 * Constructor.
		 * @param source the source observable
		 * @param bufferSize the allowed buffer size
		 * @param time the time value to wait between buffer fills
		 * @param unit the time unit
		 * @param pool the pool where to schedule the buffer splits
		 */
		public WithSizeOrTime(
				Observable<? extends T> source,
				int bufferSize,
				long time, 
				TimeUnit unit, 
				Scheduler pool) {
			this.unit = unit;
			this.time = time;
			this.pool = pool;
			this.source = source;
			this.bufferSize = bufferSize;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
			final AtomicInteger bufferLength = new AtomicInteger();
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			final DefaultRunnable r = new DefaultRunnable(lock) {
				@Override
				public void onRun() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					bufferLength.addAndGet(-curr.size());
					observer.next(curr);
				}
			};
			DefaultObserverEx<T> s = new DefaultObserverEx<T>(lock, true) {
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				public void onFinish() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					bufferLength.addAndGet(-curr.size());
					observer.next(curr);

					observer.finish();
				}

				/** The buffer to fill in. */
				@Override
				public void onNext(T value) {
					buffer.add(value);
					if (bufferLength.incrementAndGet() == bufferSize) {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());

						observer.next(curr);
					}
				}
				@Override
				public void init() {
					add("timer", pool.schedule(r, time, time, unit));
				}
			};
			return s.registerWith(source);
		}
	}
	/**
	 * Project the source sequence to
	 * potentially overlapping buffers whose
	 * start is determined by skip and lengths
	 * by size.
	 * @param <T> the type of the elements
	 * @author akarnokd, 2013.01.13.
	 */
	public static final class WithSizeSkip<T> implements Observable<List<T>> {
		/** The buffer max size. */
		private final int bufferSize;
		/** The source sequence. */
		private final Observable<? extends T> source;
		/** The skip count. */
		protected final int skip;
		/**
		 * Constructor.
		 * @param source the source observable
		 * @param bufferSize the target buffer size
		 * @param skip the number of items to skip between buffers
		 */
		public WithSizeSkip(
				Observable<? extends T> source, 
				int bufferSize,
				int skip) {
			this.bufferSize = bufferSize;
			this.source = source;
			this.skip = skip;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			return (new DefaultObserverEx<T>() {
				/** The queue of open windows. */
				@GuardedBy("lock")
				final Queue<List<T>> queue = new LinkedList<List<T>>();
				/** The current element index. */
				@GuardedBy("lock")
				int i;
				@Override
				protected void onNext(T value) {
					for (List<T> s : queue) {
						s.add(value);
					}
					int c = i - bufferSize + 1;
					if (c >= 0 && c % skip == 0) {
						List<T> s = queue.poll();
						if (!s.isEmpty()) {
							observer.next(s);
						}
					}
					
					i++;
					if (i % skip == 0) {
						List<T> s = new ArrayList<T>(bufferSize);
						queue.add(s);
						observer.next(s);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					queue.clear();
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					while (!queue.isEmpty()) {
						List<T> s = queue.poll();
						if (!s.isEmpty()) {
							observer.next(s);
						}
					}
					observer.finish();
				}
				@Override
				protected void onRegister() {
					List<T> s = new ArrayList<T>(bufferSize);
					queue.add(s);
					observer.next(s);
				}
			}).registerWith(source);
		}
	}
	/**
	 * Buffer parts of the source until the window observable finishes.
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 */
	public static class WithClosing<T, U> implements Observable<List<T>> {
		/** The source observable. */
		@Nonnull
		protected final Observable<? extends T> source;
		/** The buffer closing selector for each registerer. */
		@Nonnull
		protected final Func0<? extends Observable<U>> bufferClosingSelector;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param bufferClosingSelector the window selector
		 */
		public WithClosing(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Func0<? extends Observable<U>> bufferClosingSelector) {
			this.source = source;
			this.bufferClosingSelector = bufferClosingSelector;
			
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The buffer. */
				List<T> buffer = new ArrayList<T>();

				@Override
				protected void onNext(T value) {
					buffer.add(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					buffer = new ArrayList<T>();
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					flush();
					observer.finish();
				}
				/** Flush the buffer's value. */
				protected void flush() {
					List<T> b = buffer;
					buffer = new ArrayList<T>();
					observer.next(b);
				}
				/**
				 * Callout from the inner observable.
				 * @param ex the exception
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				@Override
				public void init() {
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
							flush();
							init();
						}
						
					};
					
					add("windowClosing", closeObserver.registerWith(bufferClosingSelector.invoke()));
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Projects the incoming values into multiple buffers based on
	 * when a window-open fires an event and a window-close finishes.
	 * An incoming value might end up in multiple buffers if their window
	 * overlaps.
	 * <p>Exception semantics: if any Observable throws an error, the whole
	 * process terminates with error.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the buffer opening selector type
	 * @param <V> the buffer closing element type (irrelevant)
	 */
	public static class WithOpenClose<T, U, V> implements Observable<List<T>> {
		/** The source sequence. */
		protected Observable<? extends T> source;
		/** The window-open observable. */
		protected Observable<? extends U> windowOpening;
		/** The function that returns a window-close observable for a value from the window-open. */
		protected Func1<? super U, ? extends Observable<V>> windowClosing;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param windowOpening the window-open observable
		 * @param windowClosing the function that returns a window-close observable
		 * for a value from the window-open
		 */
		public WithOpenClose(
				@Nonnull Observable<? extends T> source,
				@Nonnull Observable<? extends U> windowOpening,
				@Nonnull Func1<? super U, ? extends Observable<V>> windowClosing) {
			this.source = source;
			this.windowOpening = windowOpening;
			this.windowClosing = windowClosing;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			
			final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(lock, true) {
				/** The open buffers map. */
				@GuardedBy("lock")
				Map<Unique<U>, List<T>> openMap = new HashMap<Unique<U>, List<T>>();
				@Override
				protected void onNext(T value) {
					for (List<T> p : openMap.values()) {
						p.add(value);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					for (List<T> p : openMap.values()) {
						observer.next(p);
						observer.finish();
					}
					openMap = new HashMap<Unique<U>, List<T>>();
				}
				/** Relay the inner error. */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Initialize the window-open listener. */
				@Override
				public void init() {
					DefaultObserverEx<U> wo = new DefaultObserverEx<U>(lock, false) {
						@Override
						protected void onNext(U value) {
							final Unique<U> token = Unique.of(value); 
							DefaultObserverEx<V> wc = new DefaultObserverEx<V>(lock, true) {

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
									List<T> buf = openMap.remove(token);
									observer.next(buf);
								}
								
							};
							openMap.put(token, new ArrayList<T>());
							
							Observable<V> co = windowClosing.invoke(value);
							add(token, wc.registerWith(co));
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							remove(this);
						}
						
					};
					add("windowOpening", wo.registerWith(windowOpening));
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Buffers the source elements into non-overlapping lists separated
	 * by notification values from the boundary observable and its finish event.
	 * <p>Exception semantics: if any Observable throws an error, the whole
	 * process terminates with error.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 */
	public static class WithBoundary<T, U> implements Observable<List<T>> {
		/** The source observable. */
		@Nonnull
		protected final Observable<? extends T> source;
		/** The buffer closing selector for each registerer. */
		@Nonnull
		protected final Observable<U> boundary;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param boundary the notification source of the boundary
		 */
		public WithBoundary(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Observable<U> boundary) {
			this.source = source;
			this.boundary = boundary;
			
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The buffer. */
				List<T> buffer = new ArrayList<T>();

				@Override
				protected void onNext(T value) {
					buffer.add(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					buffer = new ArrayList<T>();
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					flush();
					observer.finish();
				}
				/** Flush the buffer's value. */
				protected void flush() {
					List<T> b = buffer;
					buffer = new ArrayList<T>();
					observer.next(b);
				}
				/**
				 * Callout from the inner observable.
				 * @param ex the exception
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Inner completion. */
				protected void innerFinish() {
					finish();
				}
				@Override
				public void init() {
					DefaultObserverEx<U> closeObserver = new DefaultObserverEx<U>(lock, true) {
						@Override
						protected void onNext(U value) {
							flush();
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							innerFinish();
						}
						
					};
					
					add("boundary", closeObserver.registerWith(boundary));
				}
			};
			return obs.registerWith(source);
		}
	}
}
