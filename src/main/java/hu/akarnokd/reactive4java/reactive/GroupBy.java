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
import hu.akarnokd.reactive4java.base.GroupedObservable;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.ComparingHashMap;
import hu.akarnokd.reactive4java.util.DefaultGroupedObservable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Group the specified source according to the keys provided by the extractor function
 * and compute key equality by the comparer function.
 * The resulting observable gets notified once a new group is encountered.
 * Each previously encountered group by itself receives updates along the way.
 * If the source finish(), all encountered group will finish().
 * <p>Exception semantics: if the source sends an exception, the group observable and the individual groups'
 * observables receive this error.</p>
 * @author akarnokd, 2013.01.15.
 * @param <Key> the group key type
 * @param <U> the output element type
 * @param <T> the source element type
 * @since 0.97
 */
public class GroupBy<Key, U, T> implements
		Observable<GroupedObservable<Key, U>> {
	/** */
	private final Observable<? extends T> source;
	/** */
	private final Func1<? super T, ? extends Key> keyExtractor;
	/** */
	protected final Func2<? super Key, ? super Key, Boolean> keyComparer;
	/** */
	private final Func1<? super T, ? extends U> valueExtractor;

	/**
	 * Constructor.
	 * @param source the source sequence
	 * @param keyExtractor the key extractor
	 * @param keyComparer the key comparator
	 * @param valueExtractor the value extractor
	 */
	public GroupBy(Observable<? extends T> source,
			Func1<? super T, ? extends Key> keyExtractor,
			Func2<? super Key, ? super Key, Boolean> keyComparer,
			Func1<? super T, ? extends U> valueExtractor) {
		this.source = source;
		this.keyExtractor = keyExtractor;
		this.keyComparer = keyComparer;
		this.valueExtractor = valueExtractor;
	}

	@Override
	@Nonnull 
	public Closeable register(
			@Nonnull final Observer<? super GroupedObservable<Key, U>> observer) {
		final ComparingHashMap<Key, DefaultGroupedObservable<Key, U>> knownGroups = new ComparingHashMap<Key, DefaultGroupedObservable<Key, U>>(keyComparer);
		
		return (new DefaultObserverEx<T>() {
			@Override
			public void onError(@Nonnull Throwable ex) {
				for (Observer<U> group : knownGroups.values()) {
					group.error(ex);
				}
				observer.error(ex);
			}

			@Override
			public void onFinish() {
				for (Observer<U> group : knownGroups.values()) {
					group.finish();
				}
				observer.finish();
			}

			@Override
			public void onNext(T value) {
				final Key key = keyExtractor.invoke(value);
				DefaultGroupedObservable<Key, U> group = knownGroups.get(key);
				if (group == null) {
					group = new DefaultGroupedObservable<Key, U>(key);
					knownGroups.put(key, group);
					observer.next(group);
				}
				group.next(valueExtractor.invoke(value));
			}
		}).registerWith(source);
	}
}
