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

import java.io.Closeable;

import javax.annotation.Nonnull;

import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

/**
 * Helper class for where like operations.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class Where {
	/** Helper class. */
	private Where() { }
	/**
	 * Filters the elements of the source sequence which
	 * is assignable to the provided type.
	 * @author akarnokd, 2013.01.15.
	 * @param <T> the target element type
	 */
	public static class OfType<T> implements Observable<T> {
		/** */
		private final Observable<?> source;
		/** */
		private final Class<T> clazz;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param clazz the class token
		 */
		public OfType(@Nonnull Observable<?> source, @Nonnull Class<T> clazz) {
			this.source = source;
			this.clazz = clazz;
			
		}
		@Override
		@Nonnull
		public Closeable register(final Observer<? super T> observer) {
			return source.register(new Observer<Object>() {

				@Override
				public void next(Object value) {
					if (clazz.isInstance(value)) {
						observer.next(clazz.cast(value));
					}
				}

				@Override
				public void error(Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}
				
			});
		}
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class Indexed<T> implements
			Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func2<? super Integer, ? super T, Boolean> clause;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
		 */
		public Indexed(
				Observable<? extends T> source,
				Func2<? super Integer, ? super T, Boolean> clause) {
			this.source = source;
			this.clause = clause;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The current element index. */
				int index;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (clause.invoke(index, value)) {
						observer.next(value);
					}
					index++;
				}

			});
		}
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class LongIndexed<T> implements
			Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func2<? super Long, ? super T, Boolean> clause;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
		 */
		public LongIndexed(
				Observable<? extends T> source,
				Func2<? super Long, ? super T, Boolean> clause) {
			this.source = source;
			this.clause = clause;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The current element index. */
				long index;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (clause.invoke(index, value)) {
						observer.next(value);
					}
					index++;
				}

			});
		}
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class Simple<T> implements
			Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func1<? super T, Boolean> clause;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param clause the filter clause
		 */
		public Simple(Observable<? extends T> source,
				Func1<? super T, Boolean> clause) {
			this.source = source;
			this.clause = clause;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (clause.invoke(value)) {
						observer.next(value);
					}
				}

			});
		}
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * The clauseFactory is used for each individual registering observer.
	 * This can be used to create memorizing filter functions such as distinct.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class IndexedFactory<T> implements Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func0<? extends Func2<? super Integer, ? super T, Boolean>> clauseFactory;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param clauseFactory the filter clause, the first parameter receives the current index, the second receives the current element
		 */
		public IndexedFactory(
				Observable<? extends T> source,
				Func0<? extends Func2<? super Integer, ? super T, Boolean>> clauseFactory) {
			this.source = source;
			this.clauseFactory = clauseFactory;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The current element index. */
				int index;
				/** The clause instance to use. */
				final Func2<? super Integer, ? super T, Boolean> clause = clauseFactory.invoke();
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					if (clause.invoke(index, value)) {
						observer.next(value);
					}
					index++;
				}

			});
		}
	}
}
