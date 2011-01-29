/*
 * Copyright 2011 David Karnok
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

package hu.akarnokd.reactiv4java;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Helper class with function types.
 * @author karnokd
 */
public final class Functions {
	/** Utility class. */
	private Functions() {
		// TODO Auto-generated constructor stub
	}
	/** Constant function returning always false. */
	private static final Func1<Boolean, Void> FALSE1 = new Func1<Boolean, Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return false;
		}
	};
	/** Constant function returning always true. */
	private static final Func1<Boolean, Void> TRUE1 = new Func1<Boolean, Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return true;
		}
	};
	/** Constant parameterless function which returns always false. */
	public static final Func0<Boolean> FALSE = new Func0<Boolean>() {
		@Override
		public Boolean invoke() {
			return false;
		}
	};
	/** Constant parameterless function which returns always true. */
	public static final Func0<Boolean> TRUE = new Func0<Boolean>() {
		@Override
		public Boolean invoke() {
			return true;
		}
	};
	/** The identity function which returns its parameter. */
	private static final Func1<Object, Object> IDENTITY = new Func1<Object, Object>() {
		@Override
		public Object invoke(Object param1) {
			return param1;
		}
	};
	/**
	 * Returns a function which always returns true regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always true
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<Boolean, T> alwaysTrue() {
		return (Func1<Boolean, T>)TRUE1;
	}
	/**
	 * Returns a function which always returns false regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always false
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<Boolean, T> alwaysFalse() {
		return (Func1<Boolean, T>)FALSE1;
	}
	/**
	 * Returns a function which returns its parameter value.
	 * @param <T> the type of the object
	 * @return the function which returns its parameter as is
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<T, T> identity() {
		return (Func1<T, T>)IDENTITY;
	}
	/**
	 * Creates an integer iterator which returns numbers from the start position in the count size.
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static Iterable<Integer> range(final int start, final int count) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					int current = start;
					final boolean down = count < 0;
					@Override
					public boolean hasNext() {
						return down ? current > start + count : current < start + count;
					}
					@Override
					public Integer next() {
						if (hasNext()) {
							if (down) {
								return current--;
							}
							return current++;
						}
						throw new NoSuchElementException();
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	/**
	 * Creates an long iterator which returns numbers from the start position in the count size.
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static Iterable<Long> range(final long start, final long count) {
		return new Iterable<Long>() {
			@Override
			public Iterator<Long> iterator() {
				return new Iterator<Long>() {
					long current = start;
					final boolean down = count < 0;
					@Override
					public boolean hasNext() {
						return down ? current > start + count : current < start + count;
					}
					@Override
					public Long next() {
						if (hasNext()) {
							if (down) {
								return current--;
							}
							return current++;
						}
						throw new NoSuchElementException();
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
