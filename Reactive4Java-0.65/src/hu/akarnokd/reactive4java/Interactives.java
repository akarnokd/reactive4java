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
package hu.akarnokd.reactive4java;

import hu.akarnokd.reactive4java.Interactives.LinkedBuffer.N;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
					@Override
					public boolean hasNext() {
						return current < start + count;
					}
					@Override
					public Integer next() {
						if (hasNext()) {
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
					@Override
					public boolean hasNext() {
						return current < start + count;
					}
					@Override
					public Long next() {
						if (hasNext()) {
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
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicate the predicate
	 * @return the new iterable
	 */
	public static <T> Iterable<T> where(final Iterable<? extends T> source, final Func2<Boolean, Integer, ? super T> predicate) {
		return where(source, Functions.constant0(predicate));
	}
	/**
	 * Creates an iterable which filters the source iterable with the
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * Use this construct if you want to use some memorizing predicat function (e.g., filter by subsequent distinct, filter by first occurrences only)
	 * which need to be invoked per iterator() basis.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicateFactory the predicate factory which should return a new predicate function for each iterator.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> where(final Iterable<? extends T> source, final Func0<? extends Func2<Boolean, Integer, ? super T>> predicateFactory) {
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
				final Func2<Boolean, Integer, ? super T> predicate = predicateFactory.invoke();
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<T>() {
					/** The current element count. */
					int count;
					/** The temporary store for peeked elements. */
					final SingleContainer<T> peek = new SingleContainer<T>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (it.hasNext()) {
								T value = it.next();
								if (predicate.invoke(count, value)) {
									peek.add(value);
									count++;
									return true;
								}
								count++;
							}
							return false;
						}
						return true;
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take();
						}
						throw new NoSuchElementException(); 
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
	 * Creates an iterable which filters the source iterable with the
	 * given predicate function. The predicate receives the value and
	 * must return a boolean wether to accept that entry.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicate the predicate function
	 * @return the new iterable
	 */
	public static <T> Iterable<T> where(final Iterable<? extends T> source, final Func1<Boolean, ? super T> predicate) {
		return where(source, Functions.constant0(new Func2<Boolean, Integer, T>() {
			@Override
			public Boolean invoke(Integer param1, T param2) {
				return predicate.invoke(param2);
			}
		}));
	}
	/**
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source iterable
	 * @param selector the selector function
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> select(final Iterable<? extends T> source, final Func2<? extends U, Integer, ? super T> selector) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current counter. */
					int count;
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public U next() {
						return selector.invoke(count++, it.next());
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
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source iterable
	 * @param selector the selector function
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> select(final Iterable<? extends T> source, final Func1<? extends U, ? super T> selector) {
		return select(source, new Func2<U, Integer, T>() {
			@Override
			public U invoke(Integer param1, T param2) {
				return selector.invoke(param2);
			};
		});
	};
	/**
	 * Creates an iterable which returns a stream of Us for each source Ts.
	 * The iterable stream of Us is returned by the supplied selector function.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source
	 * @param selector the selector for multiple Us for each T
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> selectMany(final Iterable<? extends T> source, 
			final Func1<? extends Iterable<? extends U>, ? super T> selector) {
		/*
		 * for (T t : source) {
		 *     for (U u : selector(t)) {
		 *         yield u;
		 *     }
		 * } 
		 */
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current selected iterator. */
					Iterator<? extends U> sel;
					@Override
					public boolean hasNext() {
						if (sel == null || !sel.hasNext()) {
							while (true) {
								if (it.hasNext()) {
									sel = selector.invoke(it.next()).iterator();
									if (sel.hasNext()) {
										return true;
									}
								} else {
									break;
								}
							}
							return false;
						}
						return true;
					}

					@Override
					public U next() {
						if (hasNext()) {
							return sel.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						if (sel == null) {
							throw new IllegalStateException();
						}
						sel.remove();
					}
					
				};
			}
		};
	}
	/**
	 * Creates an iterable which traverses the source iterable,
	 * and based on the key selector, groups values extracted by valueSelector into GroupedIterables,
	 * which can be interated over later on.
	 * The equivalence of the keys are determined via reference
	 * equality and <code>equals()</code> equality.
	 * @param <T> the source element type
	 * @param <U> the result group element type
	 * @param <V> the result group keys
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @return the new iterable
	 */
	public static <T, U, V> Iterable<GroupedIterable<V, U>> groupBy(final Iterable<? extends T> source, 
			final Func1<? extends V, ? super T> keySelector, final Func1<? extends U, ? super T> valueSelector) {
		return distinctSet(new Iterable<GroupedIterable<V, U>>() {
			@Override
			public Iterator<GroupedIterable<V, U>> iterator() {
				final Map<V, DefaultGroupedIterable<V, U>> groups = new LinkedHashMap<V, DefaultGroupedIterable<V, U>>();
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<GroupedIterable<V, U>>() {
					Iterator<DefaultGroupedIterable<V, U>> groupIt;
					@Override
					public boolean hasNext() {
						return it.hasNext() || (groupIt != null && groupIt.hasNext());
					}

					@Override
					public GroupedIterable<V, U> next() {
						if (hasNext()) {
							if (groupIt == null) {
								while (it.hasNext()) {
									T t = it.next();
									V v = keySelector.invoke(t);
									U u = valueSelector.invoke(t);
									DefaultGroupedIterable<V, U> g = groups.get(v);
									if (g == null) {
										g = new DefaultGroupedIterable<V, U>(v);
										groups.put(v, g);
									}
									g.add(u);
								}
								groupIt = groups.values().iterator();
							}
							return groupIt.next(); 
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
		}, new Func1<V, GroupedIterable<V, U>>() {
			@Override
			public V invoke(GroupedIterable<V, U> param1) {
				return param1.key();
			}
			
		}, Functions.<GroupedIterable<V, U>>identity());
	}
	/**
	 * Returns an iterable which filters its elements by an unique key
	 * in a way that when multiple source items produce the same key, only
	 * the first one ever seen gets relayed further on.
	 * Key equality is computed by reference equality and <code>equals()</code>
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param <V> the key element type.
	 * @param source the source of Ts
	 * @param keySelector the key selector for only-once filtering
	 * @param valueSelector the value select for the output of the first key cases
	 * @return the new iterable
	 */
	public static <T, U, V> Iterable<U> distinctSet(final Iterable<? extends T> source, 
			final Func1<? extends V, ? super T> keySelector, final Func1<? extends U, ? super T> valueSelector) {
		return select(where(source,
		new Func0<Func2<Boolean, Integer, T>>() {
			@Override
			public Func2<Boolean, Integer, T> invoke() {
				return new Func2<Boolean, Integer, T>() {
					final Set<V> memory = new HashSet<V>();
					@Override
					public Boolean invoke(Integer index, T param1) {
						return memory.add(keySelector.invoke(param1));
					};
				};
			};
		})
		, new Func1<U, T>() {
			@Override
			public U invoke(T param1) {
				return valueSelector.invoke(param1);
			};
		});
	}
	/**
	 * Returns an iterable which filters its elements based if they vere ever seen before in
	 * the current iteration.
	 * Value equality is computed by reference equality and <code>equals()</code>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	public static <T> Iterable<T> distinctSet(final Iterable<? extends T> source) {
		return distinctSet(source, Functions.<T>identity(), Functions.<T>identity());
	}
	/**
	 * Creates an iterable which ensures that subsequent values of T are not equal  (reference and equals).
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	public static <T> Iterable<T> distinct(final Iterable<? extends T> source) {
		return where(source,
		new Func0<Func2<Boolean, Integer, T>>() {
			@Override
			public Func2<Boolean, Integer, T> invoke() {
				return new Func2<Boolean, Integer, T>() {
					/** Is this the first element? */
					boolean first = true;
					/** The last seen element. */
					T last;
					@Override
					public Boolean invoke(Integer index, T param1) {
						if (first) {
							first = false;
							last = param1;
							return true;
						}
						if (last == param1 || (last != null && last.equals(param1))) {
							last = param1;
							return false;
						}
						last = param1;
						return true;
					};
				};
			};
		});
	}
	/**
	 * Returns an iterable which prefixes the source iterable values
	 * by a constant.
	 * It is equivalent to <code>concat(singleton(value), source)</code>.
	 * @param <T> the lement type
	 * @param source the source iterable
	 * @param value the value to prefix
	 * @return the new iterable.
	 */
	public static <T> Iterable<T> startWith(Iterable<? extends T> source, final T value) {
		return concat(singleton(value), source);
	}
	/** Utility class. */
	private Interactives() {
		// utility class
	}
	/**
	 * A generator function which returns Ts based on the termination condition and the way it computes the next values.
	 * This is equivalent to:
	 * <code><pre>
	 * T value = seed;
	 * while (predicate(value)) {
	 *     yield value;
	 *     value = next(value);
	 * }
	 * </pre></code>
	 * @param <T> the element type
	 * @param seed the initial value
	 * @param predicate the predicate to terminate the process
	 * @param next the function that computes the next value.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> generate(final T seed, final Func1<Boolean, ? super T> predicate, final Func1<? extends T, ? super T> next) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					T value = seed;
					@Override
					public boolean hasNext() {
						return predicate.invoke(value);
					}

					@Override
					public T next() {
						if (hasNext()) {
							T current = value;
							value = next.invoke(value);
							return current;
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
	 * A generator function which returns Ts based on the termination condition and the way it computes the next values,
	 * but the first T to be returned is preceded by an <code>initialDelay</code> amount of wait and each
	 * subsequent element is then generated after <code>betweenDelay</code> sleep.
	 * The sleeping is blocking the current thread which invokes the hasNext()/next() methods.
	 * This is equivalent to:
	 * <code><pre>
	 * T value = seed;
	 * sleep(initialDelay);
	 * if (predicate(value)) {
	 *     yield value;
	 * }
	 * sleep(betweenDelay);
	 * while (predicate(value)) {
	 *     yield value;
	 *     value = next(value);
	 *     sleep(betweenDelay);
	 * }
	 * </pre></code>
	 * @param <T> the element type
	 * @param seed the initial value
	 * @param predicate the predicate to terminate the process
	 * @param next the function that computes the next value.
	 * @param initialDelay the initial delay
	 * @param betweenDelay the between delay
	 * @param unit the time unit for initialDelay and betweenDelay
	 * @return the new iterable
	 */
	public static <T> Iterable<T> generate(final T seed, final Func1<Boolean, ? super T> predicate, 
			final Func1<? extends T, ? super T> next,
			final long initialDelay, final long betweenDelay, final TimeUnit unit) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					T value = seed;
					/** Keeps track of whether there should be an initial delay? */
					boolean shouldInitialWait = true;
					/** Keeps track of whether there should be an initial delay? */
					boolean shouldBetweenWait;
					@Override
					public boolean hasNext() {
						if (shouldInitialWait) {
							shouldInitialWait = false;
							try {
								unit.sleep(initialDelay);
							} catch (InterruptedException e) {
								return false; // FIXME not soure about this
							}
						} else {
							if (shouldBetweenWait) {
								shouldBetweenWait = false;
								try {
									unit.sleep(betweenDelay);
								} catch (InterruptedException e) {
									return false; // FIXME not soure about this
								}
							}
						}
						return predicate.invoke(value);
					}

					@Override
					public T next() {
						if (hasNext()) {
							shouldBetweenWait = true;
							T current = value;
							value = next.invoke(value);
							return current;
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
	 * Transforms the sequence of the source iterable into an option sequence of
	 * Option.some(), Option.none() and Option.error() values, depending on
	 * what the source's hasNext() and next() produces.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code> for its <code>remove()</code> method.
	 * @param <T> the source element type
	 * @param source the source of at least Ts.
	 * @return the new iterable
	 */
	public static <T> Iterable<Option<T>> materialize(final Iterable<? extends T> source) {
		return new Iterable<Option<T>>() {
			@Override
			public Iterator<Option<T>> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<Option<T>>() {
					/** The peeked value or exception. */
					final SingleContainer<Option<T>> peek = new SingleContainer<Option<T>>();
					/** The source iterator threw an exception. */
					boolean broken;
					@Override
					public boolean hasNext() {
						if (!broken) {
							try {
								if (peek.isEmpty()) {
									if (it.hasNext()) {
										T t = it.next();
										peek.add(Option.some(t));
									} else {
										peek.add(Option.<T>none());
										broken = true;
									}
								}
							} catch (Throwable t) {
								broken = true;
								peek.add(Option.<T>error(t));
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Option<T> next() {
						if (hasNext()) {
							return peek.take();
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
	 * Convert the source materialized elements into normal iterator behavior.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code> for its <code>remove()</code> method.
	 * @param <T> the source element types
	 * @param source the source of T options
	 * @return the new iterable
	 */
	public static <T> Iterable<T> dematerialize(final Iterable<? extends Option<? extends T>> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Option<? extends T>> it = source.iterator();
				return new Iterator<T>() {
					final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							if (it.hasNext()) {
								Option<? extends T> o = it.next();
								if (Option.isNone(o)) {
									return false;
								}
								peek.add(o);
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Merges a bunch of iterable streams where each of the iterable will run by
	 * a scheduler and their events are merged together in a single stream.
	 * The returned iterator throws an <code>UnsupportedOperationException</code> in its <code>remove()</code> method.
	 * @param <T> the element type
	 * @param sources the iterable of source iterables.
	 * @param scheduler the scheduler for running each inner iterable in parallel
	 * @return the new iterable
	 */
	public static <T> Iterable<T> merge(final Iterable<? extends Iterable<? extends T>> sources, final Scheduler scheduler) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final BlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
				final AtomicInteger wip = new AtomicInteger(1);
				final List<Closeable> handlers = new LinkedList<Closeable>();
				for (final Iterable<? extends T> iter : sources) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							try {
								for (T t : iter) {
									if (!Thread.currentThread().isInterrupted()) {
										queue.add(Option.some(t));
									}
								}
								if (wip.decrementAndGet() == 0) {
									if (!Thread.currentThread().isInterrupted()) {
										queue.add(Option.<T>none());
									}
								}
							} catch (Throwable t) {
								queue.add(Option.<T>error(t));
							}
						}
					};
					wip.incrementAndGet();
					handlers.add(scheduler.schedule(r));
				}
				if (wip.decrementAndGet() == 0) {
					queue.add(Option.<T>none());
				}
				return new Iterator<T>() {
					final SingleContainer<Option<T>> peek = new SingleContainer<Option<T>>();
					/** Are we broken? */
					boolean broken;
					@Override
					public boolean hasNext() {
						if (!broken) {
							if (peek.isEmpty()) {
								try {
									Option<T> t = queue.take();
									if (Option.isSome(t)) {
										peek.add(t);
									} else
									if (Option.isError(t)) {
										peek.add(t);
										broken = true;
									}
								} catch (InterruptedException ex) {
									return false; // FIXME not sure about this
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							try {
								return peek.take().value();
							} catch (RuntimeException ex) {
								for (Closeable h : handlers) {
									close0(h);
								}
								throw ex;
							}
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
	 * Merges a bunch of iterable streams where each of the iterable will run by
	 * a scheduler and their events are merged together in a single stream.
	 * The returned iterator throws an <code>UnsupportedOperationException</code> in its <code>remove()</code> method.
	 * @param <T> the element type
	 * @param sources the iterable of source iterables.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> merge(final Iterable<? extends Iterable<? extends T>> sources) {
		return merge(sources, Observables.getDefaultScheduler());
	}
	/**
	 * Invoke the <code>close()</code> method on the closeable instance
	 * and throw away any <code>IOException</code> it might raise.
	 * @param c the closeable instance, <code>null</code>s are simply ignored
	 */
	static void close0(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ex) {
				
			}
		}
	}
	/**
	 * Merges two iterable streams.
	 * @param <T> the element type
	 * @param first the first iterable
	 * @param second the second iterable
	 * @return the resulting stream of Ts
	 */
	public static <T> Iterable<T> merge(final Iterable<? extends T> first, final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return merge(list);
	}
	/**
	 * Creates an iterator which attempts to re-iterate the source if it threw an exception.
	 * <code><pre>
	 * while (count-- > 0) {
	 * 	  try {
	 *        for (T t : source) {
	 *            yield t;
	 *        }
	 *        break;
	 *    } catch (Throwable t) {
	 *        if (count <= 0) {
	 *            throw t;
	 *        }
	 *    }
	 * }
	 * </pre></code>
	 * @param <T> the source type
	 * @param source the source of Ts
	 * @param count the number of retry attempts
	 * @return the new iterable
	 */
	public static <T> Iterable<T> retry(final Iterable<? extends T> source, final int count) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The retry count. */
					int retries = count;
					/** The peek store. */
					final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
					/** The current iterator. */
					Iterator<? extends T> it = source.iterator();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (it.hasNext()) {
								try {
									peek.add(Option.some(it.next()));
									break;
								} catch (Throwable t) {
									if (retries-- > 0) {
										it = source.iterator();
									} else {
										peek.add(Option.<T>error(t));
										break;
									}
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take().value();
						}
						throw new NoSuchElementException();
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
	 * Counts the elements of the iterable source.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	public static <T> Iterable<Integer> count(final Iterable<T> source) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				final Iterator<T> it = source.iterator();
				return new Iterator<Integer>() {
					/** The peek ahead container. */
					final SingleContainer<Option<Integer>> peek = new SingleContainer<Option<Integer>>();
					/** Computation already done. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							if (peek.isEmpty()) {
								int count = 0;
								try {
									while (it.hasNext()) {
										it.next();
										count++;
									}
									peek.add(Option.some(count));
								} catch (Throwable t) {
									peek.add(Option.<Integer>error(t));
								} finally {
									done = true;
								} 
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Integer next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Creates an iterable which if iterates over the sourec and encounters an exception,
	 * the iteration is continued on the new iterable returned by the handler function.
	 * @param <T> the element type
	 * @param source the source iterable.
	 * @param handler the exception handler.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> catchException(final Iterable<? extends T> source, final Func1<? extends Iterable<? extends T>, ? super Throwable> handler) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The current iterator. */
					Iterator<? extends T> it = source.iterator();
					/** The peek ahead container. */
					final SingleContainer<T> peek = new SingleContainer<T>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (true) {
								try {
									if (it.hasNext()) {
										peek.add(it.next());
									}
									break;
								} catch (Throwable t) {
									it = handler.invoke(t).iterator();
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take();
						}
						throw new NoSuchElementException();
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
	 * Creates an iterable which if iterates over the sourec and encounters an exception, it simply stops the iteration, consuming the exception.
	 * @param <T> the element type
	 * @param source the source iterable.
	 * @return the new iterable
	 */
	public static <T> Iterable<T> catchException(final Iterable<? extends T> source) {
		Iterable<? extends T> e = empty();
		return catchException(source, Functions.constant(e));
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * @param <T> the source element type
	 * @param sources the list of sources to try one after another
	 * @return the new iterable
	 */
	public static <T> Iterable<T> resumeOnError(final Iterable<? extends Iterable<? extends T>> sources) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> iter0 = sources.iterator();
				if (iter0.hasNext()) {
					return new Iterator<T>() {
						/** The current iterator. */
						Iterator<? extends T> it = iter0.next().iterator();
						/** The peek ahead container. */
						final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
						@Override
						public boolean hasNext() {
							if (peek.isEmpty()) {
								while (true) {
									try {
										if (it.hasNext()) {
											peek.add(Option.some(it.next()));
										}
										break;
									} catch (Throwable t) {
										if (iter0.hasNext()) {
											it = iter0.next().iterator();
										} else {
											peek.add(Option.<T>error(t));
											break;
										}
									}
								}
							}
							return !peek.isEmpty();
						}
	
						@Override
						public T next() {
							if (hasNext()) {
								return peek.take().value();
							}
							throw new NoSuchElementException();
						}
	
						@Override
						public void remove() {
							it.remove();
						}
					};
				}
				return Interactives.<T>empty().iterator();
			}
		};
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * @param <T> the source element type
	 * @param first the first source
	 * @param second the second source
	 * @return the new iterable
	 */
	public static <T> Iterable<T> resumeOnError(final Iterable<? extends T> first, final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return resumeOnError(list);
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception or completes normally.
	 * @param <T> the source element type
	 * @param sources the list of sources to try one after another
	 * @return the new iterable
	 */
	public static <T> Iterable<T> resumeAlways(final Iterable<? extends Iterable<? extends T>> sources) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> iter0 = sources.iterator();
				if (iter0.hasNext()) {
					return new Iterator<T>() {
						/** The current iterator. */
						Iterator<? extends T> it = iter0.next().iterator();
						/** The peek ahead container. */
						final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
						@Override
						public boolean hasNext() {
							if (peek.isEmpty()) {
								while (true) {
									try {
										if (it.hasNext()) {
											peek.add(Option.some(it.next()));
											break;
										} else {
											if (iter0.hasNext()) {
												it = iter0.next().iterator();
											} else {
												break;
											}
										}
									} catch (Throwable t) {
										if (iter0.hasNext()) {
											it = iter0.next().iterator();
										} else {
											peek.add(Option.<T>error(t));
											break;
										}
									}
								}
							}
							return !peek.isEmpty();
						}
	
						@Override
						public T next() {
							if (hasNext()) {
								return peek.take().value();
							}
							throw new NoSuchElementException();
						}
	
						@Override
						public void remove() {
							it.remove();
						}
					};
				}
				return Interactives.<T>empty().iterator();
			}
		};
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * @param <T> the source element type
	 * @param first the first source
	 * @param second the second source
	 * @return the new iterable
	 */
	public static <T> Iterable<T> resumeAlways(final Iterable<? extends T> first, final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return resumeAlways(list);
	}
	/**
	 * Generates an iterable which acts like a running sum when iterating over the source iterable, e.g.,
	 * For each element in T, it computes a value by using the current aggregation value and returns it.
	 * The first call to the aggregator function will receive a zero for its first argument.
	 * @param <T> the source element type
	 * @param <U> the destination element type
	 * @param source the source of Ts
	 * @param seed the initial value of the running aggregation
	 * @param aggregator the function which takes the current running aggregation value, the current element and produces a new aggregation value.
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> scan(final Iterable<? extends T> source, final U seed, final Func2<? extends U, ? super U, ? super T> aggregator) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current value. */
					U current = seed;
					
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public U next() {
						current = aggregator.invoke(current, it.next());
						return current;
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
	 * Generates an iterable which acts like a running sum when iterating over the source iterable, e.g.,
	 * For each element in T, it computes a value by using the current aggregation value and returns it.
	 * The first call to the aggregator function will receive a zero for its first argument.
	 * @param <T> the source element type
	 * @param <U> the destination element type
	 * @param source the source of Ts
	 * @param aggregator the function which takes the current running aggregation value, the current element and produces a new aggregation value.
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> scan(final Iterable<? extends T> source, final Func2<? extends U, ? super U, ? super T> aggregator) {
		return scan(source, null, aggregator);
	}
	/**
	 * Returns the last element of the iterable or throws a <code>NoSuchElementException</code> if the iterable is empty.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the last value
	 */
	public static <T> T last(final Iterable<? extends T> source) {
		Iterator<? extends T> it = source.iterator();
		if (it.hasNext()) {
			T t = null;
			while (it.hasNext()) {
				t = it.next();
			}
			return t;
		}
		throw new NoSuchElementException();
	}
	/**
	 * A linked buffer, which can be only filled and queried.
	 * @author akarnokd, 2011.02.03.
	 * @param <T> the element type
	 */
	static class LinkedBuffer<T> {
		/** The node. */
		static class N<T> {
			/** The element value. */
			T value;
			/** The next node. */
			N<T> next;
		}
		/** The head pointer. */
		final N<T> head = new N<T>();
		/** The tail pointer. */
		N<T> tail = head;
		/** The size. */
		int size;
		/**
		 * Add a new value.
		 * @param value the new value
		 */
		public void add(T value) {
			N<T> n = new N<T>();
			n.value = value;
			tail.next = n;
			tail = n;
			size++;
		}
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the values.
	 * Note: the name is not a misspelling, see <a href='http://en.wikipedia.org/wiki/Memoization'>Memoization</a>.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	public static <T> Iterable<T> memoizeAll(final Iterable<? extends T> source) {
		final Iterator<? extends T> it = source.iterator();
		final LinkedBuffer<T> buffer = new LinkedBuffer<T>();
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The element count. */
					int count = 0;
					/** The current node pointer. */
					N<T> pointer = buffer.head;
					@Override
					public boolean hasNext() {
						return count < buffer.size || it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							if (count < buffer.size) {
								T value = pointer.next.value;
								pointer = pointer.next;
								count++;
								return value;
							} else {
								T value = it.next();
								buffer.add(value);
								count++;
								pointer = pointer.next;
								return value;
							}
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
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the values.
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> publish(final Iterable<? extends T> source, 
			final Func1<? extends Iterable<U>, ? super Iterable<T>> func) {
		return func.invoke(memoizeAll(source));
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the same cached values.
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @param initial the initial value to append to the output stream
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> publish(final Iterable<? extends T> source, 
			final Func1<? extends Iterable<? extends U>, ? super Iterable<? super T>> func, final U initial) {
		return startWith(func.invoke(memoizeAll(source)), initial);
	}
	/**
	 * A simple circular buffer with absolute indices.
	 * @author akarnokd, 2011.02.04.
	 * @param <T> the contained element type
	 */
	static class CircularBuffer<T> {
		/** The buffer. */
		final Object[] buffer;
		/** The head pointer. */
		int head;
		/** The tail pointer. */
		int tail;
		/**
		 * Construct a new circular buffer.
		 * @param size the buffer size
		 */
		public CircularBuffer(int size) {
			buffer = new Object[size];
		}
		/** @return is the buffer empty? */
		public boolean isEmpty() {
			return head == tail;
		}
		/**
		 * Add a new value to the buffer.
		 * If the buffer would overflow, it automatically removes the current head element.
		 * @param value the value
		 */
		public void add(T value) {
			buffer[(tail++) % buffer.length] = value;
			if (size() > buffer.length) {
				head++;
			}
		}
		/**
		 * Retrieve a buffer element based on an absolute index.
		 * @param index the absolute index
		 * @return the value
		 */
		@SuppressWarnings("unchecked")
		public T get(int index) {
			if (index < head) {
				throw new IllegalArgumentException("read before head");
			}
			if (index >= tail) {
				throw new IllegalArgumentException("read after tail");
			}
			return (T)buffer[index % buffer.length];
		}
		/** @return the current size of the buffer. */
		public int size() {
			return tail - head;
		}
	}
	/**
	 * Enumerates the source iterable once and caches its results.
	 * Any iterator party will basically drain this cache, e.g.,
	 * reiterating over this iterable will produce no results.
	 * Note: the name is not a misspelling, see <a href='http://en.wikipedia.org/wiki/Memoization'>Memoization</a>.
	 * FIXME not sure about the buffer sizes.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the size of the buffering
	 * @return the new iterable
	 */
	public static <T> Iterable<T> memoize(final Iterable<? extends T> source, final int bufferSize) {
		if (bufferSize < 0) {
			throw new IllegalArgumentException("bufferSize < 0");
		}
		return new Iterable<T>() {
			/** The source iterator. */
			Iterator<? extends T> it = source.iterator();
			/** The ring buffer of the memory. */
			final CircularBuffer<T> buffer = new CircularBuffer<T>(bufferSize);
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int myHead;

					@Override
					public boolean hasNext() {
						return buffer.tail > Math.max(myHead, buffer.head) || it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							if (buffer.tail == myHead) {
								T value = it.next();
								if (bufferSize > 0) {
									buffer.add(value);
								}
								myHead++;
								return value;
							} else {
								myHead = Math.max(myHead, buffer.head);
								T value = buffer.get(myHead);
								myHead++;
								return value;
							}
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
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator may only see one source element.
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> replay(final Iterable<? extends T> source, 
			final Func1<? extends Iterable<U>, ? super Iterable<T>> func) {
		return func.invoke(memoize(source, 0));
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the some cached values.
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @param bufferSize the buffer size
	 * @return the new iterable
	 */
	public static <T, U> Iterable<U> replay(final Iterable<? extends T> source, 
			final Func1<? extends Iterable<U>, ? super Iterable<T>> func, 
					final int bufferSize) {
		return func.invoke(memoize(source, bufferSize));
	}
	/**
	 * Determines if the given source has any elements at all.
	 * @param <T> the source element type, irrelevant here
	 * @param source the source of Ts
	 * @return the new iterable with a single true or false
	 */
	public static <T> Iterable<Boolean> any(final Iterable<T> source) {
		return new Iterable<Boolean>() {
			@Override
			public Iterator<Boolean> iterator() {
				return new Iterator<Boolean>() {
					/** The source's iterator. */
					Iterator<T> it = source.iterator();
					final SingleContainer<Boolean> peek = new SingleContainer<Boolean>();
					/** Query once. */
					boolean once = true;
					@Override
					public boolean hasNext() {
						if (once) {
							once = false;
							if (peek.isEmpty()) {
								peek.add(it.hasNext());
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Boolean next() {
						if (hasNext()) {
							return peek.take();
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
	 * Tests if there is any element of the source that satisfies the given predicate function.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param predicate the predicate tester function
	 * @return the new iterable
	 */
	public static <T> Iterable<Boolean> any(final Iterable<? extends T> source, final Func1<Boolean, ? super T> predicate) {
		return any(where(source, predicate));
	}
	/**
	 * Creates an iterable which traverses the source iterable and maintains a running sum value based
	 * on the <code>sum</code> function parameter. Once the source is depleted, it
	 * applies the <code>divide</code> function and returns its result.
	 * This operator is a general base for averaging (where {@code sum(u, t) => u + t}, {@code divide(u, index) => u / index}),
	 * summing (where {@code sum(u, t) => u + t}, and {@code divide(u, index) => u)}),
	 * minimum, maximum, etc.
	 * If the traversal of the source fails due an exception, that exception is reflected on the
	 * {@code next()} call of the returned iterator.
	 * @param <T> the source element type
	 * @param <U> the itermediate aggregation type
	 * @param <V> the resulting aggregation type
	 * @param source the source of Ts
	 * @param sum the function which takes the current itermediate value, 
	 * the current source value and should produce a new intermediate value.
	 * for the first element of T, the U parameter will receive null
	 * @param divide the function which takes the last intermediate value and a total count of Ts seen and should return the final aggregation value.
	 * @return the new iterable
	 */
	public static <T, U, V> Iterable<V> aggregate(final Iterable<? extends T> source, 
			final Func2<? extends U, ? super U, ? super T> sum, 
			final Func2<? extends V, ? super U, ? super Integer> divide) {
		return new Iterable<V>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The single result container. */
					final SingleContainer<Option<? extends V>> result = new SingleContainer<Option<? extends V>>(); 
					/** We have finished the aggregation. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							done = true;
							if (result.isEmpty()) {
								try {
									U intermediate = null;
									int count = 0;
									while (it.hasNext()) {
										intermediate = sum.invoke(intermediate, it.next());
										count++;
									}
									if (count > 0) {
										result.add(Option.some(divide.invoke(intermediate, count)));
									}
								} catch (Throwable t) {
									result.add(Option.<V>error(t));
								}
							}
						}
						return !result.isEmpty();
					}

					@Override
					public V next() {
						if (hasNext()) {
							return result.take().value();
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
	 * Returns an iterable which averages the source Integer values.
	 * @param source the source of Integer values
	 * @return the new iterable
	 */
	public static Iterable<Double> averageInt(Iterable<Integer> source) {
		return aggregate(source,
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Integer values.
	 * @param source the source of Integer values
	 * @return the new iterable
	 */
	public static Iterable<Double> averageLong(Iterable<Long> source) {
		return aggregate(source,
			new Func2<Double, Double, Long>() {
				@Override
				public Double invoke(Double param1, Long param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Double values.
	 * @param source the source of Double values
	 * @return the new iterable
	 */
	public static Iterable<Double> averageDouble(Iterable<Double> source) {
		return aggregate(source,
			new Func2<Double, Double, Double>() {
				@Override
				public Double invoke(Double param1, Double param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Float values.
	 * @param source the source of Float values
	 * @return the new iterable
	 */
	public static Iterable<Float> averageFloat(Iterable<Float> source) {
		return aggregate(source,
			new Func2<Float, Float, Float>() {
				@Override
				public Float invoke(Float param1, Float param2) {
					return param1 != null ? param1 + param2 : param2.floatValue();
				}
			},
			new Func2<Float, Float, Integer>() {
				@Override
				public Float invoke(Float param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source BigInteger values.
	 * @param source the source of BigInteger values
	 * @return the new iterable
	 */
	public static Iterable<BigDecimal> averageBigInteger(Iterable<BigInteger> source) {
		return aggregate(source,
			new Func2<BigInteger, BigInteger, BigInteger>() {
				@Override
				public BigInteger invoke(BigInteger param1, BigInteger param2) {
					return param1 != null ? param1.add(param2) : param2;
				}
			},
			new Func2<BigDecimal, BigInteger, Integer>() {
				@Override
				public BigDecimal invoke(BigInteger param1, Integer param2) {
					return new BigDecimal(param1).divide(new BigDecimal(param2), BigDecimal.ROUND_HALF_UP);
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source BigDecimal values.
	 * @param source the source of BigDecimal values
	 * @return the new iterable
	 */
	public static Iterable<BigDecimal> averageBigDecimal(Iterable<BigDecimal> source) {
		return aggregate(source,
			new Func2<BigDecimal, BigDecimal, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, BigDecimal param2) {
					return param1 != null ? param1.add(param2) : param2;
				}
			},
			new Func2<BigDecimal, BigDecimal, Integer>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, Integer param2) {
					return param1.divide(new BigDecimal(param2), BigDecimal.ROUND_HALF_UP);
				}
			}
		);
	}
	/**
	 * Sum the source of Integer values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<Integer> sumInt(Iterable<Integer> source) {
		return aggregate(source,
			Functions.sumInteger(), Functions.<Integer, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Long values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<Long> sumLong(Iterable<Long> source) {
		return aggregate(source,
				Functions.sumLong(), Functions.<Long, Integer>identityFirst()
			);
	}
	/**
	 * Sum the source of Float values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<Float> sumFloat(Iterable<Float> source) {
		return aggregate(source,
			Functions.sumFloat(), Functions.<Float, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Double values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<Double> sumDouble(Iterable<Double> source) {
		return aggregate(source,
			Functions.sumDouble(), Functions.<Double, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Integer values and return it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<BigInteger> sumBigInteger(Iterable<BigInteger> source) {
		return aggregate(source,
			Functions.sumBigInteger(), Functions.<BigInteger, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Integer values and return it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	public static Iterable<BigDecimal> sumBigDecimal(Iterable<BigDecimal> source) {
		return aggregate(source,
			Functions.sumBigDecimal(), Functions.<BigDecimal, Integer>identityFirst()
		);
	}
	/**
	 * Concatenates the source strings one after another and uses the given separator.
	 * @param source the source
	 * @param separator the separator to use
	 * @return the new iterable
	 */
	public static Iterable<String> join(final Iterable<?> source, final String separator) {
		return aggregate(source,
			new Func2<StringBuilder, StringBuilder, Object>() {
				@Override
				public StringBuilder invoke(StringBuilder param1, Object param2) {
					if (param1 == null) {
						param1 = new StringBuilder();
					} else {
						param1.append(separator);
					}
					param1.append(param2);
					return param1;
				}
			},
			new Func2<String, StringBuilder, Integer>() {
				@Override
				public String invoke(StringBuilder param1, Integer param2) {
					return param1.toString();
				}
			}
		);
	}
	/**
	 * Returns the minimum value of the given iterable source.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements 
	 * @return the new iterable
	 */
	public static <T extends Comparable<? super T>> Iterable<T> min(final Iterable<? extends T> source) {
		return aggregate(source, Functions.<T>min(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the maximum value of the given iterable source.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements 
	 * @return the new iterable
	 */
	public static <T extends Comparable<? super T>> Iterable<T> max(final Iterable<? extends T> source) {
		return aggregate(source, Functions.<T>max(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the minimum value of the given iterable source in respect to the supplied comparator.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements 
	 * @param comparator the comparator to use
	 * @return the new iterable
	 */
	public static <T> Iterable<T> min(final Iterable<? extends T> source, final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>min(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the maximum value of the given iterable source in respect to the supplied comparator.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements 
	 * @param comparator the comparator to use
	 * @return the new iterable
	 */
	public static <T> Iterable<T> max(final Iterable<? extends T> source, final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>max(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @param max should the computation return the minimums or the maximums
	 * @return the new iterable
	 */
	static <T, U> Iterable<List<T>> minMax(final Iterable<? extends T> source, 
			final Func1<? extends U, ? super T> keySelector, final Comparator<? super U> keyComparator, final boolean max) {
		return new Iterable<List<T>>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new Iterator<List<T>>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The single result container. */
					final SingleContainer<Option<? extends List<T>>> result = new SingleContainer<Option<? extends List<T>>>(); 
					/** We have finished the aggregation. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							done = true;
							if (result.isEmpty()) {
								try {
									List<T> intermediate = null;
									U lastKey = null;
									while (it.hasNext()) {
										T value = it.next();
										U key = keySelector.invoke(value);
										if (lastKey == null || (max ^ keyComparator.compare(lastKey, key) > 0)) {
											intermediate = new ArrayList<T>();
											lastKey = key;
										}
										intermediate.add(value);
									}
									if (intermediate != null) {
										result.add(Option.some(intermediate));
									}
								} catch (Throwable t) {
									result.add(Option.<List<T>>error(t));
								}
							}
						}
						return !result.isEmpty();
					}

					@Override
					public List<T> next() {
						if (hasNext()) {
							return result.take().value();
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
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @return the new iterable
	 */
	public static <T, U> Iterable<List<T>> maxBy(final Iterable<? extends T> source, 
			final Func1<? extends U, ? super T> keySelector, final Comparator<? super U> keyComparator) {
		return minMax(source, keySelector, keyComparator, true);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @return the new iterable
	 */
	public static <T, U> Iterable<List<T>> minBy(final Iterable<? extends T> source, 
			final Func1<? extends U, ? super T> keySelector, final Comparator<? super U> keyComparator) {
		return minMax(source, keySelector, keyComparator, false);
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type
	 * @param <U> the key type, which must be self-comparable
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @return the new iterable
	 */
	public static <T, U extends Comparable<? super U>> Iterable<List<T>> maxBy(final Iterable<? extends T> source, 
			final Func1<? extends U, ? super T> keySelector) {
		return minMax(source, keySelector, Functions.<U>comparator(), true);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type
	 * @param <U> the key type, which must be self-comparable
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @return the new iterable
	 */
	public static <T, U extends Comparable<? super U>> Iterable<List<T>> minBy(final Iterable<? extends T> source, 
			final Func1<? extends U, ? super T> keySelector) {
		return minMax(source, keySelector, Functions.<U>comparator(), false);
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type, which must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	public static <T extends Comparable<? super T>> Iterable<List<T>> maxBy(final Iterable<? extends T> source) {
		return minMax(source, Functions.<T>identity(), Functions.<T>comparator(), true);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type, which must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	public static <T extends Comparable<? super T>> Iterable<List<T>> minBy(final Iterable<? extends T> source) {
		return minMax(source, Functions.<T>identity(), Functions.<T>comparator(), false);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied comparator.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param comparator the key comparator
	 * @return the new iterable
	 */
	public static <T> Iterable<List<T>> minBy(final Iterable<? extends T> source, final Comparator<? super T> comparator) {
		return minMax(source, Functions.<T>identity(), comparator, false);
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied comparator.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param comparator the key comparator
	 * @return the new iterable
	 */
	public static <T> Iterable<List<T>> maxBy(final Iterable<? extends T> source, 
			final Comparator<? super T> comparator) {
		return minMax(source, Functions.<T>identity(), comparator, true);
	}
}