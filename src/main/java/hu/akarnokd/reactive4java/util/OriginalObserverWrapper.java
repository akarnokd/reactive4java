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

import hu.akarnokd.reactive4java.base.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps a java-observer and its associated helper observable.
 * <p>The class features a done state for the case if the observer
 * runs to completion before its close handle is registered.
 * Using the isDone() state, unwanted retention of finished observers
 * can be avoided in java-observable wrappers.
 * </p>
 * @author akarnokd, 2013.01.11.
 */
public class OriginalObserverWrapper implements
		Observer<Object> {
	/**
	 * The backing observable object. 
	 */
	protected final java.util.Observable javaObservable;
	/**
	 * The wrapped java-observer.
	 */
	protected final java.util.Observer javaObserver;
	/** Indicate that the observer run to completion. */
	protected final AtomicBoolean done = new AtomicBoolean();
	/**
	 * Constructor. Sets up the wrapping fields.
	 * @param javaObservable the backing observable
	 * @param javaObserver the java-observer to wrap
	 */
	public OriginalObserverWrapper(
			@Nullable java.util.Observable javaObservable,
			@Nonnull java.util.Observer javaObserver) {
		this.javaObservable = javaObservable;
		this.javaObserver = javaObserver;
	}

	@Override
	public void next(Object value) {
		if (!done.get()) {
			javaObserver.update(javaObservable, value);
		}
	}

	@Override
	public void error(@Nonnull Throwable ex) {
		done.set(true);
		if (javaObservable != null) {
			javaObservable.deleteObserver(javaObserver);
		}
		
	}

	@Override
	public void finish() { 
		done.set(true);
		if (javaObservable != null) {
			javaObservable.deleteObserver(javaObserver);
		}
	}
	/**
	 * @return Returns true if this observer is terminated.
	 */
	public boolean isDone() {
		return done.get();
	}
}
