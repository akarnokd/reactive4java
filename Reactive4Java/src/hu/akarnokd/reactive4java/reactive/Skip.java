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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.TimeInterval;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Helper class for skip-like operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Skip {
	/** Helper class. */
	private Skip() { }
	/**
	 * Skips the last <code>count</code> elements from the source observable.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class Last<T> implements Observable<T> {
		/** */
		private final int count;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param count the skip count
		 */
		public Last(Observable<? extends T> source, int count) {
			this.count = count;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				final Queue<T> buffer = new ConcurrentLinkedQueue<T>();

				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					buffer.add(value);
					while (buffer.size() > count) {
						observer.next(buffer.poll());
					}
				}
			});
		}
	}
	/**
	 * Skips the Ts from source while the specified condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class While<T> implements Observable<T> {
		/** */
		private final Func1<? super T, Boolean> condition;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param condition the condition that must turn false in order to start relaying
		 */
		public While(
				Observable<? extends T> source,
				Func1<? super T, Boolean> condition) {
			this.condition = condition;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** Can we relay stuff? */
				boolean mayRelay;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (!mayRelay) {
						mayRelay = !condition.invoke(value);
						if (mayRelay) {
							observer.next(value);
						}
					} else {
						observer.next(value);
					}
				}

			});
		}
	}
	/**
	 * Skips the Ts from source while the specified indexed condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class WhileIndexed<T> implements Observable<T> {
		/** */
		private final Func2<? super T, ? super Integer,  Boolean> condition;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param condition the condition that must turn false in order to start relaying
		 */
		public WhileIndexed(
				Observable<? extends T> source,
				Func2<? super T, ? super Integer, Boolean> condition) {
			this.condition = condition;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** Can we relay stuff? */
				boolean mayRelay;
				/** The current index. */
				int index;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (!mayRelay) {
						mayRelay = !condition.invoke(value, index++);
						if (mayRelay) {
							observer.next(value);
						}
					} else {
						observer.next(value);
					}
				}

			});
		}
	}
	/**
	 * Skips the Ts from source while the specified long indexed condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class WhileLongIndexed<T> implements Observable<T> {
		/** */
		private final Func2<? super T, ? super Long,  Boolean> condition;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param condition the condition that must turn false in order to start relaying
		 */
		public WhileLongIndexed(
				Observable<? extends T> source,
				Func2<? super T, ? super Long, Boolean> condition) {
			this.condition = condition;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** Can we relay stuff? */
				boolean mayRelay;
				/** The current index. */
				long index;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (!mayRelay) {
						mayRelay = !condition.invoke(value, index++);
						if (mayRelay) {
							observer.next(value);
						}
					} else {
						observer.next(value);
					}
				}

			});
		}
	}
	/**
	 * Skip the source elements until the signaller sends its first element.
	 * <p>Once the signaller sends its first value, it gets deregistered.</p>
	 * <p>Exception semantics: exceptions thrown by source or singaller is immediately forwarded to
	 * the output and the stream is terminated.</p>
	 * @param <T> the element type of the source
	 * @param <U> the element type of the signaller, irrelevant
	 * @param source the source of Ts
	 * @param signaller the source of Us
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class Until<T, U> implements Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Observable<U> signaller;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param signaller the source of Us
		 */
		public Until(Observable<? extends T> source, Observable<U> signaller) {
			this.source = source;
			this.signaller = signaller;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			final CompositeCloseable closeables = new CompositeCloseable();
			final AtomicBoolean gate = new AtomicBoolean();
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
				@Override
				protected void onClose() {
					super.onClose();
					closeables.closeSilently();
				}
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void onFinish() {
					if (gate.get()) {
						observer.finish();
					}
				}
				@Override
				public void onNext(T value) {
					if (gate.get()) {
						observer.next(value);
					}
				}
			};
			DefaultObserverEx<U> so = new DefaultObserverEx<U>(true) {
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				protected void onFinish() {
					// ignored
				}
				@Override
				public void onNext(U value) {
					gate.set(true);
					close();
				}
			};
			
			closeables.add(obs, so);
			obs.registerWith(source);
			so.registerWith(signaller);
			
			return closeables;
		}
	}
	/**
	 * Skips the given amount of next() messages from source and relays
	 * the rest.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class First<T> implements Observable<T> {
		/** */
		private final int count;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param count the number of messages to skip
		 */
		public First(Observable<? extends T> source, int count) {
			this.count = count;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The remaining count. */
				int remaining = count;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (remaining <= 0) {
						observer.next(value);
					} else {
						remaining--;
					}
				}
			});
		}
	}
	/**
	 * Skips elements of the source observable for the
	 * specified amount of time.
	 * <p>Exceptions are always forwarded immediately, even
	 * if it occurs before the skip time runs out.</p> 
	 * @author akarnokd, 2013.01.16.
	 * @param <T> the element type
	 */
	public static class FirstTimed<T> implements Observable<T> {
		/** */
		private Observable<? extends T> source;
		/** */
		private long time;
		/** */
		private TimeUnit unit;
		/** */
		private Scheduler pool;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param time the time to wait
		 * @param unit the unit
		 * @param pool the scheduler
		 */
		public FirstTimed(
				Observable<? extends T> source,
				long time,
				TimeUnit unit,
				Scheduler pool) {
			this.source = source;
			this.time = time;
			this.unit = unit;
			this.pool = pool;
		}
		@Override
		@Nonnull
		public Closeable register(final Observer<? super T> observer) {
			final AtomicBoolean go = new AtomicBoolean();
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The relay status. */
				boolean canRelay;
				@Override
				protected void onNext(T value) {
					if (!canRelay) {
						canRelay = go.get();
					}
					if (canRelay) {
						observer.next(value);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					observer.finish();
				}
			};
			obs.add("timer", pool.schedule(new DefaultRunnable() {
				@Override
				protected void onRun() {
					go.set(true);
				}
			}, time, unit));
			return obs.registerWith(source);
		}
	}
	/**
	 * Skips the elements from the end for the specified amount of time.
	 * <p>Since there is no way to know the total duration of the sequence,
	 * the operator queues elements unit they become older than the
	 * specified time, causing the elements to be delayed by time.</p>
	 * @author akarnokd, 2013.01.16.
	 * @param <T> the element type
	 */
	public static class LastTimed<T> implements Observable<T> {
		/** */
		private Observable<? extends T> source;
		/** */
		private long time;
		/** */
		private TimeUnit unit;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param time the time to skip from last
		 * @param unit the time unit
		 */
		public LastTimed(
				Observable<? extends T> source,
				long time,
				TimeUnit unit
				) {
			this.source = source;
			this.time = time;
			this.unit = unit;
		}
		@Override
		@Nonnull
		public Closeable register(final Observer<? super T> observer) {
			final long start = System.nanoTime();
			final long delta = unit.toNanos(time);
			
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The delayed queue. */
				@GuardedBy("lock")
				protected final Queue<TimeInterval<T>> queue = new LinkedList<TimeInterval<T>>();
				@Override
				protected void onNext(T value) {
					long elapsed = System.nanoTime() - start;
					queue.add(TimeInterval.of(value, elapsed));

					flush(elapsed);
				}
				/**
				 * Flush the elements of the last time interval.
				 * @param elapsed the elapsed time in nanoseconds
				 */
				protected void flush(long elapsed) {
					while (!queue.isEmpty() && elapsed - queue.peek().interval() >= delta) {
						observer.next(queue.poll().value());
					}
				}
				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					long elapsed = System.nanoTime() - start;

					flush(elapsed);
					observer.finish();
				}
				
			};
			
			return obs.registerWith(source);
		}
	}
}
