/*
 * Copyright 2011-2012 David Karnok
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

import java.io.Closeable;

/**
 * The default implementation for a connectable observable.
 * This class can be used to make a cold observable (e.g., the one which computes when a registration occurs)
 * into a hot observable (which is alive without any observers).
 * @author akarnokd, 2011.03.21.
 * @param <T> the element type to observe
 */
public class DefaultConnectableObservable<T> implements
		ConnectableObservable<T> {
	/** The source observable. */
	private final Observable<? extends T> source;
	/** The mediator observable. */
	private final Observable<? extends T> observable;
	/** The mediator observer. */
	private final Observer<? super T> observer;
	/**
	 * Construct a connectable observable. 
	 * @param source the source observable
	 * @param observable the mediator observable, this is usually the same object as the observer
	 * @param observer the mediator observer, this is usually the same object as the observable
	 */
	public DefaultConnectableObservable(Observable<? extends T> source, 
			Observable<? extends T> observable, Observer<? super T> observer) {
		this.source = source;
		this.observable = observable;
		this.observer = observer;
	}
	@Override
	public Closeable register(Observer<? super T> observer) {
		return observable.register(observer);
	}

	@Override
	public Closeable connect() {
		return source.register(observer);
	}

}
