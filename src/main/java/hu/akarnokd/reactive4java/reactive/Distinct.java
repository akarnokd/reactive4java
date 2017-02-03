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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.ComparingHashSet;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Returns distinctly keyed elements from the source.
 * @author akarnokd, 2013.01.15.
 * @param <T> the source and result element type
 * @param <U> the key type
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class Distinct<T, U> implements Observable<T> {
	/** */
	protected Observable<? extends T> source;
	/** */
	protected Func1<? super T, ? extends U> keySelector;
	/** */
	protected Func2<? super U, ? super U, Boolean> keyComparer;
	/**
	 * Constructor.
	 * @param source the source sequence
	 * @param keySelector the key selector function
	 * @param keyComparer the key comparer function
	 */
	public Distinct(
			Observable<? extends T> source, 
			Func1<? super T, ? extends U> keySelector, 
			Func2<? super U, ? super U, Boolean> keyComparer) {
		this.source = source;
		this.keySelector = keySelector;
		this.keyComparer = keyComparer;
	}
	@Override
	@Nonnull
	public Closeable register(@Nonnull final Observer<? super T> observer) {
		return source.register(new Observer<T>() {
			/** The set remembering entries. */
			ComparingHashSet<U> set = new ComparingHashSet<U>(keyComparer);
			@Override
			public void next(T value) {
				U key = keySelector.invoke(value);
				if (set.add(key)) {
					observer.next(value);
				}
			}

			@Override
			public void error(Throwable ex) {
				set = null;
				observer.error(ex);
			}

			@Override
			public void finish() {
				set = null;
				observer.finish();
			}
			
		});
	}
}
