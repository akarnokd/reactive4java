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

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.Observers;
import hu.akarnokd.reactive4java.util.Producer;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.SequentialCloseable;
import hu.akarnokd.reactive4java.util.SingleCloseable;
import hu.akarnokd.reactive4java.util.Sink;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Helper class for Reactive.delay operators.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public final class Delay {
	/** Helper class. */
	private Delay() { }
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifications.
	 * @return the delayed observable of Ts
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the element type
	 * @since 0.97
	 */
	public static class ByTime<T> implements Observable<T> {
		/** The source observable. */
		@Nonnull 
		private final Observable<? extends T> source;
		/** The wait time. */
		private final long time;
		/** The wait time unit. */
		@Nonnull 
		private final TimeUnit unit;
		/** The pool where the delay is scheduled. */
		@Nonnull 
		private final Scheduler pool;
		/** Notify observers of an error instantly? */
		private boolean instantError;
		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param time the time value
		 * @param unit the time unit
		 * @param pool the pool to use for scheduling
		 * @param instantError propagate errors instantly
		 */
		public ByTime(
				@Nonnull Observable<? extends T> source,
				long time,
				@Nonnull TimeUnit unit,
				@Nonnull Scheduler pool,
				boolean instantError) {
			this.pool = pool;
			this.unit = unit;
			this.source = source;
			this.time = time;
			this.instantError = instantError;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
				/** The outstanding requests. */
				@Nonnull 
				final BlockingQueue<Closeable> outstanding = new LinkedBlockingQueue<Closeable>();
				@Override
				public void onClose() {
					List<Closeable> list = new LinkedList<Closeable>();
					outstanding.drainTo(list);
					for (Closeable c : list) {
						Closeables.closeSilently(c);
					}
				}

				@Override
				public void onError(@Nonnull final Throwable ex) {
					if (instantError) {
						Runnable r = new OnError<T>(lock, outstanding, observer, ex, this);
						outstanding.add(pool.schedule(r, time, unit));
					} else {
						observer.error(ex);
						close();
					}
				}

				@Override
				public void onFinish() {
					Runnable r = new OnFinish<T>(lock, outstanding, observer, this);
					outstanding.add(pool.schedule(r, time, unit));
				}
				@Override
				public void onNext(final T value) {
					Runnable r = new OnNext<T>(lock, outstanding, observer, value);
					outstanding.add(pool.schedule(r, time, unit));
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Base class for the delayed observation delivery.
	 * @author akarnokd, 2013.01.12.
	 * @param <T> the element type
	 */
	public abstract static class DelayedObservation<T> extends DefaultRunnable {
		/** The observer. */
		protected final Observer<? super T> observer;
		/** The queue. */
		protected final BlockingQueue<Closeable> queue;
		
		/**
		 * Constructor.
		 * @param lock the lock
		 * @param queue the queue for deregistration
		 * @param observer the observer to interact with
		 */
		public DelayedObservation(
				@Nonnull Lock lock,
				@Nonnull BlockingQueue<Closeable> queue, 
				@Nonnull Observer<? super T> observer) {
			super(lock);
			this.queue = queue;
			this.observer = observer;
		}
		@Override
		public final void onRun() {
			try {
				onRun2();
			} finally {
				queue.poll();
			}
		}
		/** The delivery method. */
		public abstract void onRun2();
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
		 * @param lock the lock
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param value the value to deliver
		 */
		public OnNext(
				@Nonnull Lock lock,
				@Nonnull BlockingQueue<Closeable> queue,
				@Nonnull Observer<? super T> observer,
				T value) {
			super(lock, queue, observer);
			this.value = value;
		}

		@Override
		public void onRun2() {
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
		 * @param lock the lock
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param error the exception to deliver
		 * @param c the close handler to stop the entire operation
		 */
		public OnError(
				@Nonnull Lock lock,
				@Nonnull BlockingQueue<Closeable> queue,
				@Nonnull Observer<? super T> observer,
				@Nonnull Throwable error,
				@Nonnull Closeable c) {
			super(lock, queue, observer);
			this.error = error;
			this.c = c;
		}

		@Override
		public void onRun2() {
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
		 * @param lock the lock
		 * @param queue the delay-queue
		 * @param observer the observer to delay to
		 * @param c the close handler to stop the entire operation
		 */
		public OnFinish(
				@Nonnull Lock lock,
				@Nonnull BlockingQueue<Closeable> queue,
				@Nonnull Observer<? super T> observer,
				@Nonnull Closeable c) {
			super(lock, queue, observer);
			this.c = c;
		}

		@Override
		public void onRun2() {
			observer.finish();
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Delays the observable sequence based on the subscription
	 * delay and per element delay provided by a function.
	 * <p>Both registerDelay and delaySelector observables trigger
	 * the registration/delivery of the original value by next or finish calls.</p>
	 * @author akarnokd, 2013.01.16.
	 * @param <T> the source and output element type
	 */
	public static class ByObservable<T, U, V> extends Producer<T> {
		/** */
		private Observable<? extends T> source;
		/** */
		private Observable<U> registerDelay;
		/** */
		private Func1<? super T, ? extends Observable<V>> delaySelector;
		/**
		 * Constructor.
		 * @param source the source sequence.
		 * @param registerDelay the optional registration delay
		 * @param delaySelector the delay selector for each value.
		 */
		public ByObservable(@Nonnull Observable<? extends T> source,
				@Nullable Observable<U> registerDelay,
				@Nonnull Func1<? super T, ? extends Observable<V>> delaySelector
				) {
			this.source = source;
			this.registerDelay = registerDelay;
			this.delaySelector = delaySelector;
		}
		@Override
		protected Closeable run(Observer<? super T> observer, Closeable cancel,
				Action1<Closeable> setSink) {
			ResultSink sink = new ResultSink(observer, cancel);
			setSink.invoke(sink);
			return sink.run();
		}
		/** The result sink. */
		class ResultSink extends Sink<T> implements Observer<T> {
			/** The delay closeables. */
			protected final CompositeCloseable delays = new CompositeCloseable();
			/** The lock protecting the sate. */
			protected final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
			/** Source has reached its end? */
			@GuardedBy("lock")
			protected boolean atEnd;
			/** The delay registrations. */
			protected final SequentialCloseable registration = new SequentialCloseable();
			/**
			 * Constructor.
			 * @param observer the observer
			 * @param cancel the cancel token
			 */
			public ResultSink(Observer<? super T> observer, Closeable cancel) {
				super(observer, cancel);
			}
			/** @return setup the sink and return the cancellation object. */
			public Closeable run() {
				if (registerDelay == null) {
					start();
				} else {
					registration.set(Observers.registerSafe(registerDelay, new StartObserver()));
				}
				return new CompositeCloseable(registration, delays);
			}
			/** Register with the source. */
			protected void start() {
				registration.set(Observers.registerSafe(source, this));
			}
			@Override
			public void next(T value) {
				Observable<V> delay = null;
				try {
					delay = delaySelector.invoke(value);
				} catch (Throwable t) {
					error(t);
					return;
				}
				SingleCloseable d = new SingleCloseable();
				
				delays.add(d);
				
				d.set(Observers.registerSafe(delay, new DelayObserver(value, d)));
				
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				lock.lock();
				try {
					observer.get().error(ex);
					closeSilently();
				} finally {
					lock.unlock();
				}
			}
			@Override
			public void finish() {
				lock.lock();
				try {
					atEnd = true;
					Closeables.closeSilently(registration);
					checkDone();
				} finally {
					lock.unlock();
				}
			}
			/** Check if there are any outstanding events? */
			@GuardedBy("lock")
			protected void checkDone() {
				if (atEnd && delays.isEmpty()) {
					observer.get().finish();
					closeSilently();
				}
			}
			/** The delay observer. */
			class DelayObserver implements Observer<V> {
				/** The value to deliver. */
				private final T value;
				/** The self closeable. */
				private final Closeable self;
				/**
				 * Constructor.
				 * @param value the value to deliver
				 * @param self the self cancellation handler
				 */
				public DelayObserver(T value, Closeable self) {
					this.value = value;
					this.self = self;
					
				}
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.get().error(ex);
					ResultSink.this.closeSilently();
				}

				@Override
				public void finish() {
					deliver();
				}

				@Override
				public void next(V value) {
					deliver();
				}
				/** Deliver the value. */
				protected void deliver() {
					lock.lock();
					try {
						observer.get().next(this.value);
						delays.removeSilently(self);
						checkDone();
					} finally {
						lock.unlock();
					}
				}
			}
			/** The start observer. */
			class StartObserver implements Observer<U> {

				@Override
				public void error(@Nonnull Throwable ex) {
					observer.get().error(ex);
					ResultSink.this.closeSilently();
				}

				@Override
				public void finish() {
					start();
				}

				@Override
				public void next(U value) {
					start();
				}
			}
		}
	}
	/**
	 * Delays the registration to the underlying observable by
	 * a given amount.
	 * @author akarnokd, 2013.01.16.
	 * @param <T> the element type
	 */
	public static class Registration<T> implements Observable<T> {
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
		 * @param source the source observable.
		 * @param time the time to wait
		 * @param unit the time unit
		 * @param pool the scheduler pool where to wait.
		 */
		public Registration(
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
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			
			SingleCloseable wh = new SingleCloseable();
			
			final SingleCloseable r = new SingleCloseable();
			
			wh.set(pool.schedule(new DefaultRunnable() {
				@Override
				protected void onRun() {
					r.set(Observers.registerSafe(source, observer));
				}
			}, time, unit));
			
			return new CompositeCloseable(wh, r);
		}
	}
}
