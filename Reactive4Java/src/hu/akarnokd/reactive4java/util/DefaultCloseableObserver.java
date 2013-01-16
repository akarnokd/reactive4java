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

import hu.akarnokd.reactive4java.base.Cancelable;
import hu.akarnokd.reactive4java.base.CloseableObserver;
import hu.akarnokd.reactive4java.base.Observer;

import javax.annotation.Nonnull;

/**
 * Class to wrap a raw observer and provide it with 
 * enforced event-sequence constraints and closeability.
 * <ul>
 * <li>Fixed order: <code>next()* [error()|finish()]?</code></li>
 * <li>After finish or terminate, subsequent calls to the base methods are ignored</li>
 * </ul>
 * @author akarnokd, 2013.01.09.
 * @param <T> the observed element type
 * @since 0.97
 */
public class DefaultCloseableObserver<T> implements CloseableObserver<T>, Cancelable {
	/** The completion indicator. */
	protected boolean completed;
	/** The wrapped observer. */
	protected final Observer<T> observer;
	/**
	 * Constructor. Initializes the observer field.
	 * @param o the observer to wrap
	 */
	public DefaultCloseableObserver(@Nonnull Observer<T> o) {
		this.observer = o;
	}
	@Override
	public void close() {
		completed = true;
	}

	@Override
	public void next(T value) {
		if (!completed) {
			try {
				observer.next(value);
			} catch (Throwable t) {
				error(t);
			}
		}
	}

	@Override
	public void error(@Nonnull Throwable ex) {
		if (!completed) {
			try {
				observer.error(ex);
			} finally {
				close();
			}
		}
	}

	@Override
	public void finish() {
		if (!completed) {
			try {
				observer.finish();
			} finally {
				close();
			}
		}
	}
	@Override
	public boolean isClosed() {
		return completed;
	}
}
