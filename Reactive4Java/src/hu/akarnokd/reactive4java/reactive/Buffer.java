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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultRunnable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * Buffering related observable implementations.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Buffer {
	/** Helper class. */
	private Buffer() { }
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class WithTime<T> implements Observable<List<T>> {
		/**
		 * 
		 */
		private final Observable<? extends T> source;
		/**
		 * 
		 */
		private final TimeUnit unit;
		/**
		 * 
		 */
		private final long time;
		/**
		 * 
		 */
		private final Scheduler pool;

		/**
		 * @param source
		 * @param unit
		 * @param time
		 * @param pool
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
			final Lock lock = new ReentrantLock(true);

			final DefaultRunnable r = new DefaultRunnable(lock) {
				@Override
				public void onRun() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					observer.next(curr);
				}
			};
			DefaultObserver<T> o = new DefaultObserver<T>(lock, true) {
				Closeable timer = pool.schedule(r, time, time, unit);
				@Override
				protected void onClose() {
					Closeables.closeSilently(timer);
				}

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
			};
			return Closeables.newCloseable(o, source.register(o));
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class WithSizeOrTime<T> implements Observable<List<T>> {
		/**
		 * 
		 */
		private final TimeUnit unit;
		/**
		 * 
		 */
		private final long time;
		/**
		 * 
		 */
		private final Scheduler pool;
		/**
		 * 
		 */
		private final Observable<? extends T> source;
		/**
		 * 
		 */
		private final int bufferSize;

		/**
		 * @param unit
		 * @param time
		 * @param pool
		 * @param source
		 * @param bufferSize
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
			final Lock lock = new ReentrantLock(true);
			final DefaultRunnable r = new DefaultRunnable(lock) {
				@Override
				public void onRun() {
					List<T> curr = new ArrayList<T>();
					buffer.drainTo(curr);
					bufferLength.addAndGet(-curr.size());
					observer.next(curr);
				}
			};
			DefaultObserver<T> s = new DefaultObserver<T>(lock, true) {
				/** The timer companion. */
				Closeable timer = pool.schedule(r, time, time, unit);
				@Override
				protected void onClose() {
					Closeables.closeSilently(timer);
				}

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
			};
			return Closeables.newCloseable(s, source.register(s));
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class WithSize<T> implements Observable<List<T>> {
		/**
		 * 
		 */
		private final int bufferSize;
		/**
		 * 
		 */
		private final Observable<? extends T> source;

		/**
		 * @param bufferSize
		 * @param source
		 */
		public WithSize(
				Observable<? extends T> source, 
				int bufferSize) {
			this.bufferSize = bufferSize;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
			return source.register(new Observer<T>() {
				/** The current buffer. */
				List<T> buffer;

				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					if (buffer != null && buffer.size() > 0) {
						observer.next(buffer);
					}
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (buffer == null) {
						buffer = new ArrayList<T>(bufferSize);
					}
					buffer.add(value);
					if (buffer.size() == bufferSize) {
						observer.next(buffer);
						buffer = new ArrayList<T>(bufferSize);
					}
				}

			});
		}
	}

}
