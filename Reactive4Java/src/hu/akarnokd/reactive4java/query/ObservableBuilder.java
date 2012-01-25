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
package hu.akarnokd.reactive4java.query;

import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Wrapper object arount an {@code Observable} which
 * lets the user chain some Reactive operators.
 * <p>This builder is the dual of the 
 * {@link hu.akarnokd.reactive4java.query.IterableBuilder} class.</p>
 * @author akarnokd, Jan 25, 2012
 * @param <T> the element type
 */
public final class ObservableBuilder<T> implements Observable<T> {
	/** The backed observable. */
	protected final Observable<T> o;
	/**
	 * Constructor.
	 * @param source the source sequence
	 */
	protected ObservableBuilder(Observable<T> source) {
		this.o = source;
	}
	@Override
	public Closeable register(Observer<? super T> observer) {
		return o.register(observer);
	}
	public static <T> ObservableBuilder<T> from(@Nonnull Observable<T> source) {
		return new ObservableBuilder<T>(source);
	}
	public static <T> ObservableBuilder<T> from(@Nonnull Iterable<? extends T> source) {
		return from(Reactive.toObservable(source));
	}
	public static <T> ObservableBuilder<T> from(@Nonnull Iterable<? extends T> source, @Nonnull Scheduler scheduler) {
		return from(Reactive.toObservable(source, scheduler));
	}
	public static <T> ObservableBuilder<T> from(@Nonnull T... ts) {
		return from(Interactive.toIterable(ts));
	}
	public static <T> ObservableBuilder<T> from(int start, int end, @Nonnull T... ts) {
		return from(Interactive.toIterable(start, end, ts));
	}
	public static <T> ObservableBuilder<T> from(@Nonnull Scheduler scheduler, @Nonnull T... ts) {
		return from(Interactive.toIterable(ts), scheduler);
	}
	public static <T> ObservableBuilder<T> from(int start, int end, @Nonnull Scheduler scheduler, @Nonnull T... ts) {
		return from(Interactive.toIterable(start, end, ts), scheduler);
	}
	public IterableBuilder<T> toIterable() {
		return IterableBuilder.from(o);
	}
}
