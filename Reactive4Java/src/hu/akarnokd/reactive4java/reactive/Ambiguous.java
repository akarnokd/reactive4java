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
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * @author akarnokd, 2013.01.13.
 * @param <T>
 */
public final class Ambiguous<T> implements Observable<T> {
	/**
	 * 
	 */
	private final Iterable<? extends Observable<? extends T>> sources;

	/**
	 * @param sources
	 */
	public Ambiguous(Iterable<? extends Observable<? extends T>> sources) {
		this.sources = sources;
	}

	@Override
	@Nonnull
	public Closeable register(@Nonnull final Observer<? super T> observer) {
		final CompositeCloseable result = new CompositeCloseable();
		
		final List<DefaultObserverEx<T>> observers = new ArrayList<DefaultObserverEx<T>>();
		List<Observable<? extends T>> observables = new ArrayList<Observable<? extends T>>();

		final AtomicReference<Object> first = new AtomicReference<Object>();

		int i = 0;
		for (final Observable<? extends T> os : sources) {
			observables.add(os);
			final int thisIndex = i;
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
				/** We won the race. */
				boolean weWon;
				/** Cancel everyone else. */
				void cancelRest() {
					for (int i = 0; i < observers.size(); i++) {
						if (i != thisIndex) {
							observers.get(i).close();
						}
					}
				}
				/** @return Check if we won the race. */
				boolean didWeWon() {
					if (!weWon) {
						if (first.compareAndSet(null, this)) {
							weWon = true;
							cancelRest();
						} else {
							close();
						}
					}
					return weWon;
				}
				@Override
				public void onError(@Nonnull Throwable ex) {
					if (didWeWon()) {
						observer.error(ex);
					}
				}
				@Override
				public void onFinish() {
					if (didWeWon()) {
						observer.finish();
					}
				}
				@Override
				public void onNext(T value) {
					if (didWeWon()) {
						observer.next(value);
					} else {
						close();
					}
				}
			};
			observers.add(obs);
			result.add(obs);
		}
		i = 0;
		for (final Observable<? extends T> os : observables) {
			DefaultObserverEx<T> observerEx = observers.get(i);
			observerEx.registerWith(os);
			i++;
		}
		return result;
	}
}