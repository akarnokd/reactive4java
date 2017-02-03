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
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.Observers;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.sample operations.
 * @author akarnokd, 2013.01.16.
 */
public final class Sample {
	/** Helper class. */
	private Sample() { }
	/**
	 * Samples the source observable by the given time interval.
	 * The sampler uses the last observed value.
	 * @author akarnokd, 2013.01.16.
	 * @param <T> the element type
	 */
	public static class ByTime<T> implements Observable<T> {
		/** */
		private final Scheduler pool;
		/** */
		private final long time;
		/** */
		private final Observable<? extends T> source;
		/** */
		private final TimeUnit unit;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param time the time delay
		 * @param unit the time unit
		 * @param pool the scheduler pool
		 */
		public ByTime(
				Observable<? extends T> source, 
				long time,
				TimeUnit unit,
				Scheduler pool 
		) {
			this.pool = pool;
			this.time = time;
			this.source = source;
			this.unit = unit;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			final AtomicReference<T> current = new AtomicReference<T>();
			final AtomicBoolean first = new AtomicBoolean(true);
			
			final DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void onFinish() {
					observer.finish();
				}
				@Override
				public void onNext(T value) {
					first.set(false);
					current.set(value);
				}
			};
			obs.add("timer", pool.schedule(new DefaultRunnable() {
					@Override
					protected void onRun() {
						if (!first.get()) {
							observer.next(current.get());
						}
					}
				}, time, time, unit));
			return obs.registerWith(source);
		}
	}
	/**
	 * Samples the observable sequence when the other sequence
	 * fires an event. The sampling is terminated if any of
	 * the sequences finish.
	 * <p>Exception semantics: exceptions raised anywhere will
	 * terminate the sequences.</p>
	 * @author akarnokd, 2013.01.16.
	 * @param <T>
	 */
	public static class ByObservable<T, U> implements Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Observable<? extends U> sampler;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param sampler the sampler sequence
		 */
		public ByObservable(
				Observable<? extends T> source, 
				Observable<? extends U> sampler) {
			this.source = source;
			this.sampler = sampler;
		}
		@Override
		@Nonnull
		public Closeable register(Observer<? super T> observer) {
			final CompositeCloseable c = new CompositeCloseable();
			
			final AtomicReference<Object> current = new AtomicReference<Object>(EMPTY_SENTINEL);
			final AtomicBoolean done = new AtomicBoolean();
			
			c.add(Observers.registerSafe(source, new SourceObserver(current, observer, done, c)));
			
			c.add(Observers.registerSafe(sampler, new SamplerObserver(current, observer, done, c)));
			
			return c;
		}
		/**
		 * The source observer.
		 */
		class SourceObserver implements Observer<T> {
			/** */
			private final AtomicReference<Object> current;
			/** */
			private final Closeable handle;
			/** */
			private Observer<? super T> observer;
			/** */
			private AtomicBoolean done;
			/**
			 * Constructor.
			 * @param current the current value holder.
			 * @param observer the observer to send events to
			 * @param done the completion idicator.
			 * @param handle the close handle
			 */
			public SourceObserver(
					AtomicReference<Object> current, 
					Observer<? super T> observer,
					AtomicBoolean done,
					Closeable handle) {
				this.current = current;
				this.observer = observer;
				this.done = done;
				this.handle = handle;
				
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				if (done.compareAndSet(false, true)) {
					observer.error(ex);
					Closeables.closeSilently(handle);
				}
			}
			@Override
			public void finish() {
				if (done.compareAndSet(false, true)) {
					observer.finish();
					Closeables.closeSilently(handle);
				}
			}
			@Override
			public void next(T value) {
				current.set(value);
			}
		}
		/**
		 * The source observer.
		 */
		class SamplerObserver implements Observer<U> {
			/** */
			private final AtomicReference<Object> current;
			/** */
			private final Closeable handle;
			/** */
			private Observer<? super T> observer;
			/** */
			private AtomicBoolean done;
			/**
			 * Constructor.
			 * @param current the current value holder.
			 * @param observer the observer to send events to
			 * @param done the completion idicator.
			 * @param handle the close handle
			 */
			public SamplerObserver(
					AtomicReference<Object> current, 
					Observer<? super T> observer,
					AtomicBoolean done,
					Closeable handle) {
				this.current = current;
				this.observer = observer;
				this.done = done;
				this.handle = handle;
				
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				if (done.compareAndSet(false, true)) {
					observer.error(ex);
					Closeables.closeSilently(handle);
				}
			}
			@Override
			public void finish() {
				if (done.compareAndSet(false, true)) {
					observer.finish();
					Closeables.closeSilently(handle);
				}
			}
			@Override
			@SuppressWarnings("unchecked")
			public void next(U value) {
				if (!done.get()) {
					Object o = current.get();
					if (o != EMPTY_SENTINEL) {
						observer.next((T)o);
					}
				}
			}
		}
		/** The empty sentinel indicator. */
		protected static final Object EMPTY_SENTINEL = new Object();
	}
}
