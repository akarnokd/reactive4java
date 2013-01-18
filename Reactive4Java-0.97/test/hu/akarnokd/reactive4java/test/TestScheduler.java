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
package hu.akarnokd.reactive4java.test;

import static hu.akarnokd.reactive4java.base.Option.getError;
import static hu.akarnokd.reactive4java.base.Option.isError;
import static hu.akarnokd.reactive4java.base.Option.isNone;
import static hu.akarnokd.reactive4java.base.Option.isSome;
import static hu.akarnokd.reactive4java.util.Closeables.closeSilently;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.Timestamped;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.DefaultObserver;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * A {@link Scheduler} using virtual time to be used for testing time-based
 * reactive operators.
 */
public class TestScheduler implements Scheduler {

	/**
	 * Stores the current time.
	 */
	private long clock;

	/**
	 * @return the current time
	 */
	public long getClock() {
		return clock;
	}

	/**
	 * The tasks to schedule along with their timestamps.
	 */
	private final List<Timestamped<Runnable>> tasks = new ArrayList<Timestamped<Runnable>>();

	/**
	 * Schedules the given task at its timestamp.
	 * @param task the task
	 * @return handle through which the scheduling can be cancelled
	 */
	private Closeable schedule(final Timestamped<Runnable> task) {
		tasks.add(task);
		sort(tasks);
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(task);
			}
		};
	}

	@Override
	@Nonnull
	public Closeable schedule(@Nonnull Runnable run) {
		return schedule(Timestamped.of(run, getClock()));
	}

	@Override
	@Nonnull
	public Closeable schedule(@Nonnull Runnable run, long delay, @Nonnull TimeUnit unit) {
		return schedule(Timestamped.of(run, getClock() + unit.toMillis(delay)));
	}

	@Override
	@Nonnull
	public Closeable schedule(@Nonnull final Runnable run, long initialDelay, final long betweenDelay, @Nonnull final TimeUnit unit) {
		return schedule(new Runnable() {
			@Override
			public void run() {
				run.run();
				schedule(this, betweenDelay, unit);
			}
		}, initialDelay, unit);
	}

	/**
	 * @param cold whether the returned observable should be cold
	 * @param events the events to emit with timestamps
	 * @param <T> the type of the events
	 * @return an observable emitting the given events at the given timestamps
	 */
	public <T> Observable<T> createObservable(final boolean cold, final Timestamped<? extends Option<T>>... events) {
		final DefaultObservable<T> result = new TimedObservable<T>(events, cold);
		return result;
	}

	/**
	 * Convenience factory method that generates events for {@link #createObservable(boolean, Timestamped...)}.
	 * @param value the event's value
	 * @param timestamp when the event arrives
	 * @param <T> type of the event
	 * @return the timestamped event
	 */
	public static <T> Timestamped<Option.Some<T>> onNext(T value, long timestamp) {
		return Timestamped.of(Option.some(value), timestamp);
	}

	/**
	 * Convenience factory method that generates error events for {@link #createObservable(boolean, Timestamped...)}.
	 * @param ex the event's exception
	 * @param timestamp when the event arrives
	 * @param <T> type of the event
	 * @return the timestamped event
	 */
	public static <T> Timestamped<Option.Error<T>> onError(Throwable ex, long timestamp) {
		return Timestamped.of(Option.<T>error(ex), timestamp);
	}

	/**
	 * Convenience factory method that generates completion events for {@link #createObservable(boolean, Timestamped...)}.
	 * @param timestamp when the event arrives
 	 * @param <T> type of the event
	 * @return the timestamped event
	 */
	public static <T> Timestamped<Option.None<T>> onFinish(long timestamp) {
		return Timestamped.of(Option.<T>none(), timestamp);
	}

	/**
	 * Schedules the firing of the given events for the given {@link Observable}.
	 * @param observable the {@link Observable} to send the events to
	 * @param events the events to send
	 * @param <T> type of the events
	 */
	private <T> void scheduleEvents(final DefaultObservable<T> observable, Timestamped<? extends Option<T>>... events) {
		for (final Timestamped<? extends Option<T>> timestamped : events) {
			schedule(new Runnable() {
				@Override
				public void run() {
					Option<T> event = timestamped.value();
					if (isSome(event)) {
						observable.next(event.value());
					} else if (isError(event)) {
						observable.error(getError(event));
					} else if (isNone(event)) {
						observable.finish();
					}
				}
			}, timestamped.timestamp(), MILLISECONDS);
		}
	}

	/**
	 * An {@link Observable} that simply fires the explicitly given events.
	 * @param <T>
	 */
	private final class TimedObservable<T> extends DefaultObservable<T> {

		/**
		 * The events to fire.
		 */
		private final Timestamped<? extends Option<T>>[] events;

		/**
		 * Whether to send the events relative to subscription time.
		 */
		private final boolean cold;

		/**
		 * @param events the events
		 * @param cold whether the observable is cold
		 */
		private TimedObservable(Timestamped<? extends Option<T>>[] events, boolean cold) {
			this.events = events;
			this.cold = cold;
			if (!cold) {
				scheduleEvents(this, events);
			}
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull Observer<? super T> observer) {
			if (cold) {
				scheduleEvents(this, events);
			}
			return super.register(observer);
		}

	}

	/**
	 * An {@link Observer} that simply stores the observed events with timestamps.
	 * @param <T>
	 */
	private class TestableObserverImpl<T> extends DefaultObserver<T> implements TestableObserver<T> {

		/**
		 * Default constructor.
		 */
		public TestableObserverImpl() {
			super(true);
		}

		/**
		 * Stores the received events.
		 */
		private final List<Timestamped<? extends Option<T>>> events = new ArrayList<Timestamped<? extends Option<T>>>();

		@Override
		protected void onNext(T value) {
			events.add(TestScheduler.onNext(value, getClock()));
		}

		@Override
		protected void onError(@Nonnull Throwable ex) {
			events.add(TestScheduler.<T>onError(ex, getClock()));
		}

		@Override
		protected void onFinish() {
			events.add(TestScheduler.<T>onFinish(getClock()));
		}

		@Override
		public List<Timestamped<? extends Option<T>>> getEvents() {
			return unmodifiableList(events);
		}

	}

	/**
	 * @param <T> type of events
	 * @return a {@link TestableObserver} for testing
	 */
	public <T> TestableObserver<T> createObserver() {
		return new TestableObserverImpl<T>();
	}

	/**
	 * Advances the clock by the given timespan, dispatching all tasks scheduled for it.
	 * @param relativeTime duration in ticks
	 */
	public void advanceBy(long relativeTime) {
		advanceTo(getClock() + relativeTime);
	}

	/**
	 * Advances the clock to the given time, dispatching all tasks scheduled until then.
	 * @param absoluteTime instant in ticks
	 */
	public void advanceTo(long absoluteTime) {
		dispatchUntil(absoluteTime);
		clock = absoluteTime;
	}

	/**
	 * Dispatches all scheduled tasks.
	 * Warning: will execute forever if there is infinitely repeating task.
	 */
	public void start() {
		dispatchUntil(Long.MAX_VALUE);
	}

	/**
	 * Dispatches tasks scheduled until the given time.
	 * @param absoluteTime instant in ticks
	 */
	private void dispatchUntil(long absoluteTime) {
		Iterator<Timestamped<Runnable>> iterator = tasks.iterator();
		if (iterator.hasNext()) {
			Timestamped<Runnable> task = iterator.next();
			if (task.timestamp() < absoluteTime) {
				clock = task.timestamp();
				task.value().run();
				tasks.remove(task);
				dispatchUntil(absoluteTime);
			}
		}
	}

	/**
	 * Sets up and executes the common observable testing scenario.
	 * @param observableFactory creates the tested observable
	 * @param creation when to create the observable
	 * @param subscription when to subscribe to the observable
	 * @param disposal when to dispose the subscription
	 * @param unit the time unit of the timestamps
	 * @param <T> type of events
	 * @return the observer containing the fired events
	 */
	public <T> TestableObserver<T> start(final Func0<? extends Observable<T>> observableFactory, long creation, long subscription, long disposal, TimeUnit unit) {
		final AtomicReference<Observable<T>> observable = new AtomicReference<Observable<T>>();
		final AtomicReference<Closeable> closeable = new AtomicReference<Closeable>();
		final TestableObserver<T> observer = createObserver();
		schedule(new Runnable() {
			@Override
			public void run() {
				observable.set(observableFactory.invoke());
			}
		}, creation, unit);
		schedule(new Runnable() {
			@Override
			public void run() {
				closeable.set(observable.get().register(observer));
			}
		}, subscription, unit);
		schedule(new Runnable() {
			@Override
			public void run() {
				closeSilently(closeable.get());
			}
		}, disposal, unit);
		start();
		return observer;
	}

}
