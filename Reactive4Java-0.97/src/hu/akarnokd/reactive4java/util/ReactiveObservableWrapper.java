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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Wrapper for an existing reactive-observable and maps its
 * features to the java-observable.
 * <p>Removes all registered java-observers in case an error or finish is received.
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 * @param <T> the wrapped observable's element type
 */
public class ReactiveObservableWrapper<T>
extends java.util.Observable 
implements Observable<T> {
	/** The wrapped reactive-observable. */
	protected final Observable<T> observable;
	/** The registry for observer-closeable pairs. */
	@GuardedBy("this")
	protected final Map<java.util.Observer, Closeable> registry = new LinkedHashMap<java.util.Observer, Closeable>();
	/**
	 * Constructor.
	 * @param observable the reactive-observable to wrap
	 */
	public ReactiveObservableWrapper(@Nonnull Observable<T> observable) {
		this.observable = observable;
	}
	@Override
	public synchronized void addObserver(java.util.Observer o) {
		if (o == null) {
			throw new IllegalArgumentException("o is null");
		}
		OriginalObserverWrapper observer = Observers.toObserver(o, this);
		Closeable c = observable.register(observer);
		if (!observer.isDone() && !registry.containsKey(o)) {
			registry.put(o, c);
		}
	}
	@Override
	public synchronized void deleteObserver(java.util.Observer o) {
		Closeable c = registry.remove(o);
		Closeables.closeSilently(c);
	}
	@Override
	public synchronized void deleteObservers() {
		for (Closeable c : registry.values()) {
			Closeables.closeSilently(c);
		}
		registry.clear();
	}
	@Override
	public synchronized int countObservers() {
		return registry.size();
	}
	@Override
	public void notifyObservers(Object arg) {
		List<java.util.Observer> os = null;
		synchronized (this) {
			os = new ArrayList<java.util.Observer>(registry.keySet());
		}
		for (java.util.Observer o : os) {
			o.update(this, o);
		}
	}
	@Override
	@Nonnull
	public Closeable register(@Nonnull Observer<? super T> observer) {
		return observable.register(observer);
	}
	/**
	 * Registers a java-observer and returns a handle to it.
	 * The observer can be unregistered via this handle or the regular deleteObserver().
	 * <p>The convenience method is to have symmetric means
	 * for both observer kinds to interact with this observable.<p>
	 * @param observer the observer to register
	 * @return the unregistration handle
	 */
	@Nonnull 
	public Closeable register(@Nonnull final java.util.Observer observer) {
		Closeable handle = new Closeable() {
			@Override
			public void close() throws IOException {
				deleteObserver(observer);
			}
		};
		addObserver(observer);
		return handle;
	}
}
