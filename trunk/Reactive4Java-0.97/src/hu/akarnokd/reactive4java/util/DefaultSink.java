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

import java.io.Closeable;

import javax.annotation.Nonnull;

import hu.akarnokd.reactive4java.base.Observer;

/**
 * A default sink implementation which wraps
 * an observer and closes a closeable on error
 * or finish.
 * @author akarnokd, 2013.01.16.
 * @param <T> the element type
 * @since 0.97
 */
public class DefaultSink<T> extends Sink<T> implements Observer<T> {
	/**
	 * Constructor.
	 * @param observer the obserer to wrap
	 * @param cancel the cancellation callback
	 */
	public DefaultSink(Observer<? super T> observer, Closeable cancel) {
		super(observer, cancel);
	}

	@Override
	public void error(@Nonnull Throwable ex) {
		observer.get().error(ex);
		closeSilently();
	}

	@Override
	public void finish() {
		observer.get().finish();
		closeSilently();
	}

	@Override
	public void next(T value) {
		// TODO Auto-generated method stub

	}

}
