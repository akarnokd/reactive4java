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
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Base class to help implement various operators for observable sequences.
 * @param <T> the produced value type
 * @author akarnokd, 2013.01.15.
 */
public abstract class Producer<T> implements Observable<T> {
	@Override
	@Nonnull
	public Closeable register(@Nonnull Observer<? super T> observer) {
		return registerRaw(observer, true);
	}
	/**
	 * Performs the registration and wiring up cancellation handlers for
	 * the given observer.
	 * @param observer the observer that wants to register
	 * @param safeguard apply safeguards around the observer?
	 * @return the closeable handle to stop the entire processing
	 */
	protected Closeable registerRaw(@Nonnull Observer<? super T> observer, boolean safeguard) {
		State<T> state = new State<T>();
		state.observer = observer;
		state.sink = new SingleCloseable();
		state.registration = new SingleCloseable();
		
		CompositeCloseable d = new CompositeCloseable(state.sink, state.registration);
		
		if (safeguard) {
			state.observer = new SafeObserver<T>(state.observer, d);
		}
		// Rx: some current thread scheduling stuff here, ignored for now
		// the else case:
		state.registration.set(run(state.observer, state.registration, state));
		
		return d;
	}
	/**
	 * The core implementation of the operator, called upon
	 * registration to the producer.
	 * <p>Note that the observer is not automatically detached in case of
	 * error or finish cases. Implementations should ensure proper
	 * termination.</p>
	 * @param observer the observer to send notifications on
	 * @param cancel the cancellation handler from the run() call, allows self cancellations 
	 * @param setSink communicates the sink to the registering party which allows consumers
	 * to tunnel close calls into the sink, which can stop processing
	 * @return the closeable representing all resources and/or registrations to cancel at once.
	 */
	protected abstract Closeable run(Observer<? super T> observer, Closeable cancel, Action1<Closeable> setSink);
	/**
	 * The internal state of the observer registrations.
	 * @author akarnokd, 2013.01.15.
	 * @param <U> the element type
	 */
	protected static class State<U> implements Action1<Closeable> {
		/** The closeable handle to the value sink. */
		public SingleCloseable sink;
		/** The closeable handle for the registration. */
		public SingleCloseable registration;
		/** The reference to the observer. */
		public Observer<? super U> observer;
		@Override
		public void invoke(Closeable value) {
			sink.set(value);
		}
	}
}
