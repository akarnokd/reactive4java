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
import hu.akarnokd.reactive4java.base.ObservationKind;
import hu.akarnokd.reactive4java.util.ObservableToIterableAdapter;
import hu.akarnokd.reactive4java.util.ObserverToIteratorSink;
import hu.akarnokd.reactive4java.util.SingleOption;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Samples the latest T value coming from the source observable or the initial
 * value when no messages arrived so far. If the producer and consumer run
 * on different speeds, the consumer might receive the same value multiple times.
 * The iterable sequence terminates if the source finishes or returns an error.
 * <p>Note that it is possible one doesn't receive the
 * last value of a fixed-length observable sequence in case 
 * the last next() call is is quickly followed by a finish() event.</p>
 * <p>The returned iterator throws <code>UnsupportedOperationException</code> for its <code>remove()</code> method.</p>
 * @author akarnokd, 2013.01.12.
 * @param <T> the element type
 * @since 0.97
 */
public class MostRecent<T> extends ObservableToIterableAdapter<T, T> {
	/** The initial value to have. */
	protected final T initialValue;
	/**
	 * Constructor.
	 * @param observable the source observable
	 * @param initialValue the initial value
	 */
	public MostRecent(@Nonnull Observable<? extends T> observable, T initialValue) {
		super(observable);
		this.initialValue = initialValue;
	}

	@Override
	@Nonnull 
	protected ObserverToIteratorSink<T, T> run(@Nonnull Closeable handle) {
		return new ObserverToIteratorSink<T, T>(handle) {
			/** The observation kind. */
			@Nonnull 
			protected volatile ObservationKind kind = ObservationKind.NEXT;
			/** The current value. */
			protected volatile T curr = initialValue;
			/** The current error. */
			protected volatile Throwable error;
			@Override
			public void next(T value) {
				this.curr = value;
				kind = ObservationKind.NEXT;
			}

			@Override
			public void error(@Nonnull Throwable ex) {
				done();

				this.error = ex;
				kind = ObservationKind.ERROR;
			}

			@Override
			public void finish() {
				done();

				kind = ObservationKind.FINISH;
			}

			@Override
			public boolean tryNext(@Nonnull SingleOption<? super T> out) {
				switch (kind) {
				case NEXT:
					out.add(curr);
					return true;
				case ERROR:
					out.addError(error);
					return true;
				default:
				}
				return false;
			}
			
		};
	}
	
}
