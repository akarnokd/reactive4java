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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The interactive (i.e., <code>Iterable</code> based) counterparts
 * of the <code>Observables</code> operators.
 * @author akarnokd, 2011.02.02.
 */
public final class Interactives {

	/**
	 * Iterates over the given source without using its returned value. 
	 * This method is useful when the concrete values from the iterator
	 * are not needed but the iteration itself implies some side effects.
	 * @param source the source iterable to run through
	 */
	public static void run(final Iterable<?> source) {
		run(source, Actions.noAction1());
	}
	/**
	 * Iterate over the source and submit each value to the
	 * given action. Basically, a for-each loop with pluggable
	 * action.
	 * This method is useful when the concrete values from the iterator
	 * are not needed but the iteration itself implies some side effects.
	 * @param <T> the element type of the iterable
	 * @param source the iterable
	 * @param action the action to invoke on with element
	 */
	public static <T> void run(final Iterable<? extends T> source, Action1<? super T> action) {
		for (T t : source) {
			action.invoke(t);
		}
	}
	/**
	 * Construct a new iterable which will invoke the specified action
	 * before the source value gets relayed through it.
	 * Can be used to inject side-effects before returning a value.
	 * @param <T> the returned element type
	 * @param source the source iterable
	 * @param action the action to invoke before each next() is returned.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> invoke(final Iterable<? extends T> source, 
			final Action1<? super T> action) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<T>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}
					@Override
					public T next() {
						T value = it.next();
						action.invoke(value);
						return value;
					}
					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * Creates an iterable which returns only a single element.
	 * The returned iterable throws <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.
	 * @param <T> the element type
	 * @param value the value to return
	 * @return the new iterable
	 */
	public static <T> Iterable<T> singleton(final T value) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** Return the only element? */
					boolean first = true;
					@Override
					public boolean hasNext() {
						return first;
					}

					@Override
					public T next() {
						if (first) {
							first = false;
							return value;
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
	/**
	 * Creates an observer with debugging purposes. 
	 * It prints the submitted values to STDOUT separated by commas and line-broken by 80 characters, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @return the observer
	 */
	public static <T> Action1<T> print() {
		return print(", ", 80);
	}
	/**
	 * Creates an observer with debugging purposes. 
	 * It prints the submitted values to STDOUT, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @param separator the separator to use between subsequent values
	 * @param maxLineLength how many characters to print into each line
	 * @return the observer
	 */
	public static <T> Action1<T> print(final String separator, final int maxLineLength) {
		return new Action1<T>() {
			/** Indicator for the first element. */
			boolean first = true;
			/** The current line length. */
			int len;
			@Override
			public void invoke(T value) {
				String s = String.valueOf(value);
				if (first) {
					first = false;
					System.out.print(s);
					len = s.length();
				} else {
					if (len + separator.length() + s.length() > maxLineLength) {
						if (len == 0) {
							System.out.print(separator);
							System.out.print(s);
							len = s.length() + separator.length();
						} else {
							System.out.println(separator);
							System.out.print(s);
							len = s.length();
						}
					} else {
						System.out.print(separator);
						System.out.print(s);
						len += s.length() + separator.length();
					}
				}
			};
		};
	}
	/**
	 * Creates an action for debugging purposes. 
	 * It prints the submitted values to STDOUT with a line break.
	 * @param <T> the value type
	 * @return the observer
	 */
	public static <T> Action1<T> println() {
		return new Action1<T>() {
			@Override
			public void invoke(T value) {
				System.out.println(value);
			};
		};
	}
	/**
	 * Creates an action for debugging purposes. 
	 * It prints the submitted values to STDOUT with a line break.
	 * @param <T> the value type
	 * @param prefix the prefix to use when printing
	 * @return the action
	 */
	public static <T> Action1<T> println(final String prefix) {
		return new Action1<T>() {
			@Override
			public void invoke(T value) {
				System.out.print(prefix);
				System.out.println(value);
			};
		};
	}
	/**
	 * Concatenate the given iterable sources one
	 * after another in a way, that calling the second <code>iterator()</code> 
	 * only happens when there is no more element in the first iterator.
	 * @param <T> the element type
	 * @param first the first iterable
	 * @param second the second iterable
	 * @return the new iterable
	 */
	public static <T> Iterable<T> concat(final Iterable<? extends T> first, final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new LinkedList<Iterable<? extends T>>();
		list.add(first);
		list.add(second);
		return concat(list);
	}
	/**
	 * Concatenate the given iterable sources one
	 * after another in a way, that calling the second <code>iterator()</code> 
	 * only happens when there is no more element in the first iterator.
	 * @param <T> the element type
	 * @param sources the list of iterables to concatenate
	 * @return a new iterable
	 */
	public static <T> Iterable<T> concat(final Iterable<? extends Iterable<? extends T>> sources) {
		/* 
		 * for (I<T> ii : sources) {  
		 *     for (T t : ii) {
		 *         yield t;
		 *     }
		 * }     
		 */
		
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> si = sources.iterator();
				if (si.hasNext()) {
					return new Iterator<T>() {
						/** The current iterable. */
						Iterator<? extends T> iter = si.next().iterator();
						@Override
						public boolean hasNext() {
							// we have more elements in the current iterator
							if (iter.hasNext()) {
								return true;
							}
							// do we have any more iterables?
							if (si.hasNext()) {
								return true;
							}
							return false;
						}

						@Override
						public T next() {
							do {
								if (iter.hasNext()) {
									return iter.next();
								}
								if (si.hasNext()) {
									iter = si.next().iterator();
								} else {
									break;
								}
							} while (true);							
							throw new NoSuchElementException();
						}

						@Override
						public void remove() {
							iter.remove();
						}
					};
				}
				return Interactives.<T>empty().iterator();
			}
			
		};
	}
	/** The common empty iterator. */
	private static final Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public Object next() {
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			throw new IllegalStateException();
		}
	};
	/** The common empty iterable. */
	private static final Iterable<Object> EMPTY_ITERABLE = new Iterable<Object>() {
		@Override
		public Iterator<Object> iterator() {
			return EMPTY_ITERATOR;
		}
	};
	/**
	 * Returns an empty iterable which will not produce elements.
	 * Its <code>hasNext()</code> returns always false,
	 * <code>next()</code> throws a <code>NoSuchElementException</code>
	 * and <code>remove()</code> throws an <code>IllegalStateException</code>.
	 * Note that the <code>Collections.emptyIterable()</code> static method is introduced by Java 7.
	 * @param <T> the element type, irrelevant
	 * @return the iterable
	 */
	@SuppressWarnings("unchecked")
	public static <T> Iterable<T> empty() {
		return (Iterable<T>)EMPTY_ITERABLE;
	}
	/**
	 * Returns an iterator which will throw the given
	 * <code>Throwable</code> exception when the client invokes
	 * <code>next()</code> the first time. Any subsequent
	 * <code>next()</code> call will simply throw a <code>NoSuchElementException</code>.
	 * Calling <code>remove()</code> will always throw a <code>IllegalStateException</code>.
	 * If the given Throwable instance extends a <code>RuntimeException</code>, it is throws
	 * as is, but when the throwable is a checked exception, it is wrapped
	 * into a <code>RuntimeException</code>. 
	 * FIXME not sure about next() semantics
	 * @param <T> the element type, irrelevant
	 * @param t the exception to throw
	 * @return the new iterable
	 */
	public static <T> Iterable<T> throwException(final Throwable t) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** First call? */
					boolean first = true;
					@Override
					public boolean hasNext() {
						return first;
					}

					@Override
					public T next() {
						if (first) {
							first = false;
							if (t instanceof RuntimeException) {
								throw (RuntimeException)t;
							}
							throw new RuntimeException(t);
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new IllegalStateException();
					}
					
				};
			}
		};
	}
	/**
	 * Creates an iterable which filters the source iterable with the
	 * given predicate function. The predicate receives an index
	 * telling how many elements were processed thus far.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicate the predicate function
	 * @return the new iterable
	 */
	public static <T> Iterable<T> where(final Iterable<T> source, Func2<Boolean, Integer, T> predicate) {
		/*
		 * int i = 0;
		 * for (T t : source) {
		 *     if (predicate(i, t) {
		 *         yield t;
		 *     }
		 *     i++;
		 * }
		 */
		
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<T> it = source.iterator();
				return new Iterator<T>() {
					/** The current element count. */
					int count;
					@Override
					public boolean hasNext() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public T next() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void remove() {
						it.remove();
					}
					
				};
			}
		};
	}
	/** Utility class. */
	private Interactives() {
		// utility class
	}
}
