/*
 * Copyright 2011 David Karnok
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

package hu.akarnokd.reactiv4java;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A composite scheduler and observer class for
 * cases when the observation is entangled with some timed
 * schedule operation and they need to communicate
 * with each other and deregister together.
 * @author akarnokd, 2011.01.30.
 * @param <T> the observed type
 */
public abstract class DefaultRunnableObserver<T> implements RunnableClosableObserver<T> {
	/** The future holding the reference to the scheduled part. */
	protected final AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();
	/** The handler returned by the registration. */
	protected final AtomicReference<Closeable> handler = new AtomicReference<Closeable>();
//	/** The lock that helps to ensure the next(), finish() and error() are never overlapping. */
//	private final Lock lock = new ReentrantLock(true);
//	/** Helper indicator that this observer may process its events: closed ones will ignore any further events. */
//	protected final AtomicBoolean live = new AtomicBoolean(true);
	/**
	 * Schedules this instance on the given pool with the defined delay.
	 * If this instance has an associated future, that instance gets cancelled
	 * @param pool the scheduler pool
	 * @param delay the delay
	 * @param unit the time unit of the delay
	 */
	public void schedule(Scheduler pool, long delay, TimeUnit unit) {
		FutureTask<T> f = new FutureTask<T>(this, null);
		replace(f);
		pool.schedule(f, unit.toNanos(delay));
	}
	/**
	 * Replace the future registration with the current contents
	 * and cancel the old one if there was any.
	 * @param f the new future
	 * @return the new future
	 */
	Future<?> replace(Future<?> f) {
		Future<?> fold = future.getAndSet(f);
		if (fold != null) {
			fold.cancel(true);
		}
		return f;
	}
	/**
	 * Replace the current contents of the handler registration with
	 * the new one and close the old one if there was any.
	 * @param c the new Closeable
	 * @return the new Closeable
	 */
	Closeable replace(Closeable c) {
		Closeable cold = handler.getAndSet(c);
		if (cold != null) {
			try { cold.close(); } catch (IOException ex) { }
		}
		return c;
	}
	/**
	 * Registers this instance on the given pool as a repeatable task
	 * which gets repeated at a fixed rate after an initial delay.
	 * @param pool the target scheduler pool
	 * @param initialDelay the initial schedule delay
	 * @param delay the delay between runs
	 * @param unit the time unit of the initialDelay and delay parameters
	 */
	public void schedule(Scheduler pool, long initialDelay, long delay, TimeUnit unit) {
		FutureTask<T> f = new FutureTask<T>(this, null);
		replace(f);
		pool.schedule(f, unit.toNanos(initialDelay), unit.toNanos(delay));
	}
	/**
	 * Submit this task to the given executor service without any scheduling
	 * requirements.
	 * If this instance has an associated future, that instance gets cancelled
	 * @param pool the target executor service
	 */
	public void schedule(Scheduler pool) {
		FutureTask<T> f = new FutureTask<T>(this, null);
		replace(f);
		pool.schedule(f);
	}
	/**
	 * Register with the given observable and store the closeable handle
	 * of this registration. If this was registered to another observable,
	 * that registration is unregistered.
	 * @param observable the target observable
	 */
	public void register(Observable<? extends T> observable) {
		replace(observable.register(this)); // FIXME assignment delay???
	}
	/**
	 * Deregisters the current instance from the associated observer
	 * without affecting the scheduler part's registration.
	 * If you need to deregister both, use the close() method instead.
	 */
	protected void unregister() {
		replace((Closeable)null);
	}
	/**
	 * Cancel the scheduler part of this instance without cancelling
	 * the observable registration.
	 * If you need to deregister both, use the close() method instead.
	 */
	protected void cancel() {
		replace((Future<?>)null);
	}
	/** @return the convenience method for Thread.currentThread().isInterrupted(). */
	public boolean cancelled() {
		return Thread.currentThread().isInterrupted();
	}
	@Override
	public void close() {
		cancel();
		unregister();
	}
}
