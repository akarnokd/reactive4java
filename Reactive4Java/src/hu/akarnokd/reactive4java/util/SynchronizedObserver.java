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

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Class to wrap an observer's method calls via a synchronized to prevent
 * concurrent callback of its methods.
 * The class implements the Closeable interface and forwards
 * this close call to the wrapped observer, if it supports 
 * Closeable as well (otherwise, its no-op).
 * 
 * @author akarnokd, 2013.01.09.
 * @param <T> the element type
 * @since 0.97
 */
public class SynchronizedObserver<T> implements Observer<T>, Closeable {
	/** The wrapped observer. */
	protected final Observer<T> observer;
	/** The lock object. */
	protected final Object gate;
	/**
	 * Constructor, sets the wrapped observer and uses an internal object as gate.
	 * @param o the observer
	 */
	public SynchronizedObserver(@Nonnull Observer<T> o) {
		this(o, new Object());
	}
	/**
	 * Constructor, sets the wrapped observer and uses the provided lock.
	 * @param o the observer
	 * @param gate the gate object where the synchronized() will be called upon
	 */
	public SynchronizedObserver(@Nonnull Observer<T> o, @Nonnull Object gate) {
		this.observer = o;
		this.gate = gate;
	}
	/** @return the gate object */
	public Object getGate() {
		return gate;
	}
	@Override
	public void close() throws IOException {
		synchronized (gate) {
			Closeables.close(observer);
		}
	}
	@Override
	public void next(T value) {
		synchronized (gate) {
			observer.next(value);
		}
	}
	@Override
	public void error(Throwable ex) {
		synchronized (gate) {
			observer.error(ex);
		}
	}
	@Override
	public void finish() {
		synchronized (gate) {
			observer.finish();
		}
	}
}
