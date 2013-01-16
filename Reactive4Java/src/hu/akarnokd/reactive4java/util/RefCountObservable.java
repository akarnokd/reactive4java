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

/**
 * Observable which wraps an observable and manages a reference counting closeable.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 * @param <T> the element type
 */
public class RefCountObservable<T> extends Producer<T> {
	/** The wrapped observable. */
	protected final Observable<? extends T> source;
	/** The reference counter. */
	protected final RefCountCloseable refCount;
	/**
	 * Constructor.
	 * @param source the source observable
	 * @param refCount the reference counting closeable
	 */
	public RefCountObservable(Observable<? extends T> source, RefCountCloseable refCount) {
		this.source = source;
		this.refCount = refCount;
	}

	@Override
	protected Closeable run(Observer<? super T> observer, Closeable cancel,
			Action1<Closeable> setSink) {
		
		CompositeCloseable c = new CompositeCloseable(refCount.getCloseable(), cancel);
		
		DefaultSink<T> s = new DefaultSink<T>(observer, c);
		setSink.invoke(s);
		
		return Observers.registerSafe(source, s);
	}

}
