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

import hu.akarnokd.reactive4java.base.CloseableIterable;
import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.Throwables;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * @author akarnokd, 2013.01.13.
 * @param <U>
 * @param <T>
 */
public final class Collect<U, T> implements CloseableIterable<U> {
	/**
	 * 
	 */
	private final Observable<? extends T> source;
	/**
	 * 
	 */
	private final Func1<? super U, ? extends U> newCollector;
	/**
	 * 
	 */
	private final Func0<? extends U> initialCollector;
	/**
	 * 
	 */
	private final Func2<? super U, ? super T, ? extends U> merge;

	/**
	 * @param source
	 * @param newCollector
	 * @param initialCollector
	 * @param merge
	 */
	public Collect(
			Observable<? extends T> source,
			Func0<? extends U> initialCollector,
			Func2<? super U, ? super T, ? extends U> merge,
			Func1<? super U, ? extends U> newCollector
			) {
		this.source = source;
		this.newCollector = newCollector;
		this.initialCollector = initialCollector;
		this.merge = merge;
	}

	@Override
	public CloseableIterator<U> iterator() {
		final AtomicReference<U> collector = new AtomicReference<U>(initialCollector.invoke());
		final AtomicBoolean done = new AtomicBoolean();
		final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
		
		final DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
			@Override
			protected void onError(@Nonnull Throwable ex) {
				error.set(ex);
				done.set(true);
			}

			@Override
			protected void onFinish() {
				done.set(true);
			}

			@Override
			protected void onNext(T value) {
				U current = collector.get();
				current = merge.invoke(current, value);
				collector.set(current);
			}
			
		};
		obs.registerWith(source);
		
		return new CloseableIterator<U>() {
			/** The current value received by hasNext(). */
			U currentValue;
			/** Have we completed as well? */
			boolean completed;
			@Override
			public void close() throws IOException {
				obs.close();
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			@Override
			public boolean hasNext() {
				if (!completed) {
					currentValue = collector.get();
				}
				return completed;
			}

			@Override
			public U next() {
				if (hasNext()) {
					if (done.get()) {
						completed = true;
						Throwables.throwAsUnchecked(error.get());
					} else {
						collector.set(newCollector.invoke(currentValue));
					}
					return currentValue;
				}
				throw new NoSuchElementException();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}