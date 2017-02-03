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
import hu.akarnokd.reactive4java.util.DefaultGroupedObservable;
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.groupByUntil operators.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class GroupByUntil {
	/** Helper class. */
	private GroupByUntil() { }
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p>The key comparison is done by the <code>Object.equals()</code> semantics of the <code>HashMap</code>.</p>
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class Default<K, V, T, D> implements
			Observable<GroupedObservable<K, V>> {
		/** */
		protected final Func1<? super T, ? extends V> valueSelector;
		/** */
		protected final Observable<? extends T> source;
		/** */
		protected final Func1<? super T, ? extends K> keySelector;
		/** */
		protected final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param keySelector the key selector
		 * @param valueSelector the value selector
		 * @param durationSelector the group duration selector.
		 */
		public Default(
				Observable<? extends T> source,
				Func1<? super T, ? extends K> keySelector,
				Func1<? super T, ? extends V> valueSelector,
				Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector) {
			this.valueSelector = valueSelector;
			this.source = source;
			this.keySelector = keySelector;
			this.durationSelector = durationSelector;
		}

		@Override
		@Nonnull 
		public Closeable register(
				@Nonnull final Observer<? super GroupedObservable<K, V>> observer) {
			DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
				/** The active groups. */
				final Map<K, DefaultGroupedObservable<K, V>> groups = new HashMap<K, DefaultGroupedObservable<K, V>>();
				@Override
				protected void onError(@Nonnull Throwable ex) {
					for (Observer<V> o : groups.values()) {
						o.error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					for (Observer<V> o : groups.values()) {
						o.finish();
					}
					observer.finish();
				}

				@Override
				protected void onNext(T value) {
					final K k = keySelector.invoke(value);
					final V v = valueSelector.invoke(value);
					DefaultGroupedObservable<K, V> gr = groups.get(k);
					if (gr == null) {
						gr = new DefaultGroupedObservable<K, V>(k);
						final DefaultGroupedObservable<K, V> fgr = gr;
						groups.put(k, gr);
						add(fgr, durationSelector.invoke(gr).register(new DefaultObserver<D>(lock, true) {

							@Override
							protected void onError(@Nonnull Throwable ex) {
								fgr.error(ex); // FIXME error propagation
								groups.remove(k);
								remove(fgr);
							}

							@Override
							protected void onFinish() {
								fgr.finish();
								groups.remove(k);
								remove(fgr);
							}

							@Override
							protected void onNext(D value) {
								fgr.finish();
								groups.remove(k);
								remove(fgr);
							}

						}));
						observer.next(gr);
					}
					gr.next(v);
				}

			};
			o.registerWith(source);
			return o;
		}
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class WithComparer<K, V, T, D> implements
			Observable<GroupedObservable<K, V>> {
		/** */
		private final Func1<? super T, ? extends V> valueSelector;
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func2<? super K, ? super K, Boolean> keyComparer;
		/** */
		private final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector;
		/** */
		private final Func1<? super T, ? extends K> keySelector;
		/** The key class with custom equality comparer. */
		class Key {
			/** The key value. */
			final K key;
			/**
			 * Constructor.
			 * @param key the key
			 */
			Key(K key) {
				this.key = key;
			}
			@Override
			@SuppressWarnings("unchecked")
			public boolean equals(Object obj) {
				if (obj instanceof WithComparer.Key) {
					return keyComparer.invoke(key, ((Key)obj).key);
				}
				return false;
			}
			@Override
			public int hashCode() {
				return key != null ? key.hashCode() : 0;
			}
		}
		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param keySelector the key extractor
		 * @param valueSelector the value extractor
		 * @param durationSelector the observable for a particular group termination
		 * @param keyComparer the key comparer for the grouping
		 */
		public WithComparer(
				Observable<? extends T> source,
				Func1<? super T, ? extends K> keySelector,
				Func1<? super T, ? extends V> valueSelector,
				Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector,
				Func2<? super K, ? super K, Boolean> keyComparer
			) {
			this.valueSelector = valueSelector;
			this.source = source;
			this.keyComparer = keyComparer;
			this.durationSelector = durationSelector;
			this.keySelector = keySelector;
		}

		@Override
		@Nonnull 
		public Closeable register(
				@Nonnull final Observer<? super GroupedObservable<K, V>> observer) {
			DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
				/** The active groups. */
				final Map<Key, DefaultGroupedObservable<K, V>> groups = new HashMap<Key, DefaultGroupedObservable<K, V>>();
				@Override
				protected void onError(@Nonnull Throwable ex) {
					for (Observer<V> o : groups.values()) {
						o.error(ex);
					}
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					for (Observer<V> o : groups.values()) {
						o.finish();
					}
					observer.finish();
				}
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}

				@Override
				protected void onNext(T value) {
					final K kv = keySelector.invoke(value);
					final Key k = new Key(kv);
					final V v = valueSelector.invoke(value);
					DefaultGroupedObservable<K, V> gr = groups.get(k);
					if (gr == null) {
						gr = new DefaultGroupedObservable<K, V>(kv);
						final DefaultGroupedObservable<K, V> fgr = gr;
						groups.put(k, gr);
						add(fgr, durationSelector.invoke(gr).register(new DefaultObserver<D>(lock, true) {

							@Override
							protected void onError(@Nonnull Throwable ex) {
								innerError(ex);
								groups.remove(k);
								remove(fgr);
							}

							@Override
							protected void onFinish() {
								fgr.finish();
								groups.remove(k);
								remove(fgr);
							}

							@Override
							protected void onNext(D value) {
								fgr.finish();
								groups.remove(k);
								remove(fgr);
							}

						}));
						observer.next(gr);
					}
					gr.next(v);
				}

			};
			o.registerWith(source);
			return o;
		}
	}
}
