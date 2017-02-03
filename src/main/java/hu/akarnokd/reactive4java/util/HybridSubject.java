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

import hu.akarnokd.reactive4java.base.CloseableObservable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Subject;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

/**
 * A combination of the java and reactive observable and observer
 * classes and interfaces.
 * <p>Note that since java-observer is not generic, update() calls
 * are cast to type T and might cause ClassCastException in case
 * the data traveling is incorrect.</p>
 * <p>If the reactive-observer receives an error or finish event,
 * all sub-observers are notified and both kinds of observers are
 * unregistered.</p>
 * <p>Note also that reactive-observers are not notified in registration
 * order and mixed observer case, only the java-observers are notified
 * in order after the notification of reactive-observers.</p>
 * @param <T> the value type
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 */
public class HybridSubject<T> 
extends
	java.util.Observable 
implements 
	java.util.Observer,
	Subject<T, T>, CloseableObservable<T> {
	/** The registration holder for the reactive-observers. */
	@Nonnull 
	protected ConcurrentMap<Closeable, Observer<? super T>> registry = new ConcurrentHashMap<Closeable, Observer<? super T>>();
	@Override
	public void next(T value) {
		for (Observer<? super T> o : observers()) {
			o.next(value);
		}
		setChanged();
		super.notifyObservers(value);
	}
	@Override
	@SuppressWarnings("unchecked")
	public void notifyObservers(Object arg) {
		next((T)arg);
	}
	@Override
	public void error(@Nonnull Throwable ex) {
		for (Observer<? super T> o : observers()) {
			o.error(ex);
		}
		close();
	}

	@Override
	public void finish() {
		for (Observer<? super T> o : observers()) {
			o.finish();
		}
		close();
	}
	/**
	 * @return a snapshot of the registered observers.
	 */
	@Nonnull 
	protected List<Observer<? super T>> observers() {
		return new ArrayList<Observer<? super T>>(registry.values());
	}
	@Override
	@Nonnull
	public Closeable register(
			@Nonnull Observer<? super T> observer) {
		Closeable handle = new Closeable() {
			@Override
			public void close() throws IOException {
				unregister(this);
			}
		};
		registry.put(handle, observer);
		return handle;
	}
	/**
	 * Registers a java-observer and returns a handle to it.
	 * The observer can be unregistered via this handle or the regular deleteObserver().
	 * <p>The convenience method is to have symmetric means
	 * for both observer kinds to interact with this observable.<p>
	 * @param observer the observer to register
	 * @return the unregistration handle
	 */
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
	/**
	 * Unregister the given observer by its handle.
	 * @param handle the handle identifying the observer
	 */
	protected void unregister(@Nonnull Closeable handle) {
		registry.remove(handle);
	}

	@Override
	public void update(java.util.Observable o, Object arg) {
		@SuppressWarnings("unchecked") T t = (T)arg;
		next(t);
	}
	@Override
	public void deleteObservers() {
		close();
	}
	@Override
	public void close() {
		registry.clear();
		super.deleteObservers();
	}
	@Override
	public synchronized int countObservers() {
		return super.countObservers() + registry.size();
	}
	/**
	 * Registers the reactive-observer with this observable.
	 * <p>The convenience method is to have symmetric means
	 * for both observer kinds to interact with this observable.<p>
	 * @param observer the reactive-observer to register
	 */
	public void addObserver(@Nonnull Observer<? super T> observer) {
		register(observer);
	}
	/**
	 * Deregisters all instances of the supplied observer.
	 * <p>Note that reactive-registration allows multiple
	 * registration and deregistration for the same observer instance,
	 * which are treated independently whereas java-observable does not.</p>
	 * <p>The convenience method is to have symmetric means
	 * for both observer kinds to interact with this observable.<p>
	 * @param observer the reactive-observer to unregister
	 */
	public void deleteObserver(@Nonnull Observer<? super T> observer) {
		registry.values().removeAll(Collections.singleton(observer));
	}
}
