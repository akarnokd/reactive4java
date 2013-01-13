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
package hu.akarnokd.reactive4java.reactive;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.util.ObservableToIterableAdapter;
import hu.akarnokd.reactive4java.util.ObserverToIteratorSink;
import hu.akarnokd.reactive4java.util.SingleOption;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Convert the given observable instance into a classical iterable instance.
 * <p>The resulting iterable does not support the {@code remove()} method.</p>
 * @param <T> the element type to iterate
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class ToIterable<T> extends
		ObservableToIterableAdapter<T, T> {
	/**
	 * Constructor.
	 * @param observable the observable to convert
	 */
	public ToIterable(Observable<? extends T> observable) {
		super(observable);
	}

	@Override
	protected ObserverToIteratorSink<T, T> run(Closeable handle) {
		return new ObserverToIteratorSink<T, T>(handle) {
			/** The queue. */
			final BlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
			@Override
			public void next(T value) {
				queue.add(Option.some(value));
			}

			@Override
			public void error(Throwable ex) {
				done();
				
				queue.add(Option.<T>error(ex));
			}

			@Override
			public void finish() {
				done();

				queue.add(Option.<T>none());
			}

			@Override
			public boolean tryNext(SingleOption<? super T> out) {
				try {
					Option<T> o = queue.take();
					
					if (Option.isNone(o)) {
						return false;
					}
					out.addOption(o);
				} catch (InterruptedException ex) {
					out.addError(ex);
				}
				return true;
			}
			
		};
	}
}
