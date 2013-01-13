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
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
 * The delay preserves the relative time difference between subsequent notifications.
 * @return the delayed observable of Ts
 * @author akarnokd, 2013.01.12.
 * @param <T> the element type
 * @since 0.97
 */
public final class Delay<T> implements Observable<T> {
	/** The source observable. */
	private final Observable<? extends T> source;
	/** The wait time. */
	private final long time;
	/** The wait time unit. */
	private final TimeUnit unit;
	/** The pool where the delay is scheduled. */
	private final Scheduler pool;
	/**
	 * Constructor.
	 * @param source the source of Ts
	 * @param time the time value
	 * @param unit the time unit
	 * @param pool the pool to use for scheduling
	 */
	public Delay(
			@Nonnull Observable<? extends T> source,
			long time,
			@Nonnull TimeUnit unit,
			@Nonnull Scheduler pool) {
		this.pool = pool;
		this.unit = unit;
		this.source = source;
		this.time = time;
	}

	@Override
	public Closeable register(final Observer<? super T> observer) {
		DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
			/** The outstanding requests. */
			final BlockingQueue<Closeable> outstanding = new LinkedBlockingQueue<Closeable>();
			@Override
			public void onClose() {
				List<Closeable> list = new LinkedList<Closeable>();
				outstanding.drainTo(list);
				for (Closeable c : list) {
					Closeables.closeSilently(c);
				}
				super.close();
			}

			@Override
			public void onError(final Throwable ex) {
				Runnable r = new OnError<T>(outstanding, observer, ex, this);
				outstanding.add(pool.schedule(r, time, unit));
			}

			@Override
			public void onFinish() {
				Runnable r = new OnFinish<T>(outstanding, observer, this);
				outstanding.add(pool.schedule(r, time, unit));
			}
			@Override
			public void onNext(final T value) {
				Runnable r = new OnNext<T>(outstanding, observer, value);
				outstanding.add(pool.schedule(r, time, unit));
			}
		};
		return obs.registerWith(source);
	}
	/**
	 * Base class for the delayed observation delivery.
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the element type
	 */
	public abstract static class DelayedObservation<T> implements Runnable {
		/** The observer. */
		protected final Observer<? super T> observer;
		/** The queue. */
		protected final BlockingQueue<Closeable> queue;
		/**
		 * Constructor.
		 * @param queue the queue for deregistration
		 * @param observer the observer to interact with
		 */
		public DelayedObservation(
				@Nonnull BlockingQueue<Closeable> queue, 
				@Nonnull Observer<? super T> observer) {
			this.queue = queue;
			this.observer = observer;
		}
		@Override
		public final void run() {
			try {
				onRun();
			} finally {
				queue.poll();
			}
		}
		/** The delivery method. */
		public abstract void onRun();
	}
	/**
	 * Deliver a next() value.
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the element type
	 */
	public static class OnNext<T> extends DelayedObservation<T> {
		/** The value to deliver. */
		private final T value;
		/**
		 * Constructor.
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param value the value to deliver
		 */
		public OnNext(
				BlockingQueue<Closeable> queue,
				Observer<? super T> observer,
				T value) {
			super(queue, observer);
			this.value = value;
		}

		@Override
		public void onRun() {
			observer.next(value);
		}
		
	}
	/**
	 * Deliver an error() event.
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the value type.
	 */
	public static class OnError<T> extends DelayedObservation<T> {
		/** The exception to deliver. */
		@Nonnull 
		private final Throwable error;
		/** The close handler to stop the entire operation. */
		@Nonnull 
		private final Closeable c;
		/**
		 * Constructor.
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param error the exception to deliver
		 * @param c the close handler to stop the entire operation
		 */
		public OnError(
				BlockingQueue<Closeable> queue,
				Observer<? super T> observer,
				Throwable error,
				Closeable c) {
			super(queue, observer);
			this.error = error;
			this.c = c;
		}

		@Override
		public void onRun() {
			observer.error(error);
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Deliver a finish() event.
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the element type
	 */
	public static class OnFinish<T> extends DelayedObservation<T> {
		/** The close handler to stop the entire operation. */
		@Nonnull 
		private final Closeable c;
		/**
		 * Constructor.
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param c the close handler to stop the entire operation
		 */
		public OnFinish(
				@Nonnull BlockingQueue<Closeable> queue,
				@Nonnull Observer<? super T> observer,
				@Nonnull Closeable c) {
			super(queue, observer);
			this.c = c;
		}

		@Override
		public void onRun() {
			observer.finish();
			Closeables.closeSilently(c);
		}
	}
}
