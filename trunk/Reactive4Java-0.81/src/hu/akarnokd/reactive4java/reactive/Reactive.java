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

package hu.akarnokd.reactive4java.reactive;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Actions;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.util.CircularBuffer;
import hu.akarnokd.reactive4java.util.DefaultScheduler;
import hu.akarnokd.reactive4java.util.SingleLaneExecutor;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;


/**
 * Utility class with operators and helper methods for reactive programming with <code>Observable</code>s and <code>Observer</code>s.
 * Guidances were taken from 
 * <ul>
 * <li><a href='http://theburningmonk.com/tags/rx/'>http://theburningmonk.com/tags/rx/</a></li>
 * <li><a href='http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx'>http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx</a></li>
 * <li><a href='http://reactive4java.googlecode.com/svn/trunk/Reactive4Java/docs/javadoc/hu/akarnokd/reactive4java/reactive/Reactive.html'>http://reactive4java.googlecode.com/svn/trunk/Reactive4Java/docs/javadoc/hu/akarnokd/reactive4java/reactive/Reactive.html</a></li>
 * <li><a href='http://rxwiki.wikidot.com/101samples#toc3'>http://rxwiki.wikidot.com/101samples#toc3</a></li>
 * <li><a href='http://channel9.msdn.com/Tags/rx'>http://channel9.msdn.com/Tags/rx</a></li>
 * </ul>
 * 
 * @author akarnokd, 2011.01.26
 * @see hu.akarnokd.reactive4java.interactive.Interactive
 */
public final class Reactive {

	/**
	 * A variant of the registering observable which stores a group key.
	 * @author akarnokd, 2011.01.29.
	 * @param <Key> the type of the key
	 * @param <Value> the value type
	 */
	static class GroupedRegisteringObservable<Key, Value> extends DefaultObservable<Value> implements GroupedObservable<Key, Value> {
		/** The group key. */
		private final Key key;
		/**
		 * Constructor.
		 * @param key the group key
		 */
		public GroupedRegisteringObservable(Key key) {
			this.key = key;
		}
		@Override
		public Key key() {
			return key;
		}
	}
	/** The diagnostic states of the current runnable. */
	public enum ObserverState { OBSERVER_ERROR, OBSERVER_FINISHED, OBSERVER_RUNNING }
	/** The common observable pool where the Observer methods get invoked by default. */
	static final AtomicReference<Scheduler> DEFAULT_SCHEDULER = new AtomicReference<Scheduler>(new DefaultScheduler());
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate((x, y) => x + y)</code> produces a sequence of [0, 1, 3, 6, 10];<br>
	 * basically the first event (0) is just relayed and then every pair of values are simply added together and relayed
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> scan(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func2<? extends T, ? super T, ? super T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current;
					/** Are we waiting for the first value? */
					boolean first = true;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void finish() {
						observer.finish();
					}
					@Override
					public void next(T value) {
						if (first) {
							first = false;
							current = value;
							
						} else {
							current = accumulator.invoke(current, value);
						}
						observer.next(current);
					}
				});
			}
		};
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11];<br>
	 * basically the accumulation starts from zero and the first value (0) that comes in is simply added 
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> scan(
			@Nonnull final Observable<? extends T> source, 
			final T seed, 
			@Nonnull final Func2<? extends T, ? super T, ? super T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current = seed;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void finish() {
						observer.finish();
					}
					@Override
					public void next(T value) {
						current = accumulator.invoke(current, value);
						observer.next(current);
					}
				});
			}
		};
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(1, 5).accumulate0(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11, 16];<br>
	 * basically, it submits the seed value (1) and computes the current aggregate with the current value(1). 
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> scan0(
			@Nonnull final Observable<? extends T> source, 
			final T seed, 
			@Nonnull final Func2<? extends T, ? super T, ? super T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current;
					/** Are we waiting for the first value? */
					boolean first = true;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void finish() {
						observer.finish();
					}
					@Override
					public void next(T value) {
						if (first) {
							first = false;
							observer.next(seed);
							current = accumulator.invoke(seed, value);
						} else {
							current = accumulator.invoke(current, value);
						}
						observer.next(current);
					}
				});
			}
		};
	}
	/**
	 * Returns an observable which provides a TimeInterval of Ts which
	 * records the elapsed time between successive elements.
	 * The time interval is evaluated using the System.nanoTime() differences
	 * as nanoseconds
	 * The first element contains the time elapsed since the registration occurred.
	 * @param <T> the time source
	 * @param source the source of Ts
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<TimeInterval<T>> addTimeInterval(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<TimeInterval<T>>() {
			@Override
			public Closeable register(final Observer<? super TimeInterval<T>> observer) {
				return source.register(new Observer<T>() {
					long lastTime = System.nanoTime();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						long t2 = System.nanoTime();
						observer.next(TimeInterval.of(value, t2 - lastTime));
						lastTime = t2;
					}
					
				});
			}
		};
	}
	/**
	 * Wrap the values within a observable to a timestamped value having always
	 * the System.currentTimeMillis() value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public static <T> Observable<Timestamped<T>> addTimestamped(
			@Nonnull Observable<? extends T> source) {
		return select(source, Reactive.<T>wrapTimestamped());
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <T> the element type
	 * @param source the source observable
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull 
	public static <T> Observable<T> aggregate(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func2<? extends T, ? super T, ? super T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current aggregation result. */
					T result;
					/** How many items did we get */
					int phase;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					};
					@Override
					public void finish() {
						if (phase >= 1) { // FIXME not sure about this
							observer.next(result);
						}
						observer.finish();
					}
					@Override
					public void next(T value) {
						if (phase == 0) {
							result = value;
							phase++;
						} else {
							result = accumulator.invoke(result, value);
							phase = 2;
						}
					}
				});
			}
		};
	}
	/**
	 * Computes an aggregated value of the source Ts by applying a sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <T> the type of the values
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @param source the source of BigDecimals to aggregate.
	 * @param sum the function which sums the input Ts. The first received T will be acompanied by a null U.
	 * @param divide the function which perform the final division based on the number of elements
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static <T, U, V> Observable<V> aggregate(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func2<? extends U, ? super U, ? super T> sum, 
			@Nonnull final Func2<? extends V, ? super U, ? super Integer> divide) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				return source.register(new Observer<T>() {
					/** The number of values. */
					int count;
					/** The sum of the values thus far. */
					U temp;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (count > 0) {
							observer.next(divide.invoke(temp, count));
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						temp = sum.invoke(temp, value); 
						count++;
					}
					
				});
			}
		};
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <T> the input element type
	 * @param <U> the ouput element type
	 * @param source the source observable
	 * @param seed the initial value of the accumulator
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull 
	public static <T, U> Observable<U> aggregate(
			@Nonnull final Observable<? extends T> source, 
			final U seed, 
			@Nonnull final Func2<? extends U, ? super U, ? super T> accumulator) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The current aggregation result. */
					U result = seed;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					};
					@Override
					public void finish() {
						observer.next(result);
						observer.finish();
					}
					@Override
					public void next(T value) {
						result = accumulator.invoke(result, value);
					}
				});
			}
		};
	}
	/**
	 * Signals a single true or false if all elements of the observable matches the predicate.
	 * It may return early with a result of false if the predicate simply does not match the current element.
	 * For a true result, it waits for all elements of the source observable.
	 * @param <T> the type of the source data
	 * @param source the source observable
	 * @param predicate the predicate to setisfy
	 * @return the observable resulting in a single result
	 */
	@Nonnull 
	public static <T> Observable<Boolean> all(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<Boolean, ? super T> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<T>() {
					/** Indicate if we returned early. */
					boolean done;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					};
					@Override
					public void finish() {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					}
					@Override
					public void next(T value) {
						if (!predicate.invoke(value)) {
							done = true;
							observer.next(false);
							observer.finish();
						}
					}
				});
			}
		};
	}
	/**
	 * Channels the values of the first observable who fires first from the given set of observables.
	 * E.g., <code>O3 = Amb(O1, O2)</code> if O1 starts to submit events first, O3 will relay these events and events of O2 will be completely ignored
	 * @param <T> the type of the observed element
	 * @param sources the iterable list of source observables.
	 * @return the observable which reacted first
	 */
	@Nonnull 
	public static <T> Observable<T> amb(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final List<DefaultObserver<T>> observers = new ArrayList<DefaultObserver<T>>();

				List<Observable<? extends T>> observables = new ArrayList<Observable<? extends T>>();
				
				final AtomicReference<Object> first = new AtomicReference<Object>();
				int i = 0;
				for (final Observable<? extends T> os : sources) {
					observables.add(os);
					final int thisIndex = i;
					DefaultObserver<T> obs = new DefaultObserver<T>(true) {
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
						public void onError(Throwable ex) {
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
						};
					};
					observers.add(obs);
				}
				i = 0;
				List<Closeable> closers = new ArrayList<Closeable>(observables.size() * 2 + 1);
				for (final Observable<? extends T> os : observables) {
					DefaultObserver<T> dob = observers.get(i);
					closers.add(dob);
					closers.add(os.register(dob)); // FIXME deregister?!
					
					i++;
				}
				return closeAll(closers);
			}
		};
	}
	/**
	 * Signals a single true if the source observable contains any element.
	 * It might return early for a non-empty source but waits for the entire observable to return false. 
	 * @param <T> the element type
	 * @param source the source
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<Boolean> any(
			@Nonnull final Observable<T> source) {
		return any(source, Functions.alwaysTrue());
	}
	/**
	 * Signals a single TRUE if the source signals any next() and the value matches the predicate before it signals a finish().
	 * It signals a false otherwise. 
	 * @param <T> the source element type.
	 * @param source the source observable
	 * @param predicate the predicate to test the values
	 * @return the observable.
	 */
	@Nonnull 
	public static <T> Observable<Boolean> any(
			@Nonnull final Observable<T> source, 
			@Nonnull final Func1<Boolean, ? super T> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					public void onFinish() {
						observer.next(false);
						observer.finish();
						close();
					}

					@Override
					public void onNext(T value) {
						if (predicate.invoke(value)) {
							observer.next(true);
							observer.finish();
							close();
						}
					}
					
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Wraps the given observer into an action object which then dispatches
	 * various incoming Option values to next(), finish() and error().
	 * @param <T> the element type
	 * @param observer the observer to wrap
	 * @return the wrapper action
	 */
	@Nonnull 
	public static <T> Action1<Option<T>> asAction(
			@Nonnull final Observer<? super T> observer) {
		return new Action1<Option<T>>() {
			@Override
			public void invoke(Option<T> value) {
				dispatch(observer, value);
			}
		};
	}
	/**
	 * Convert the Observable instance into a functional-observable object.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @return the action to action to option of T
	 */
	@Nonnull 
	public static <T> Action1<Action1<Option<T>>> asFObservable(
			@Nonnull final Observable<? extends T> source) {
		return new Action1<Action1<Option<T>>>() {
			@Override
			public void invoke(final Action1<Option<T>> o) {
				source.register(asObserver(o));
			}
		};
	}
	/**
	 * Convert the given observable instance into a classical iterable instance.
	 * @param <T> the element type to iterate
	 * @param observable the original observable
	 * @return the iterable
	 */
	@Nonnull 
	public static <T> Iterable<T> asIterable(
			@Nonnull final Observable<? extends T> observable) {
		return asIterable(observable, DEFAULT_SCHEDULER.get()); 
	}
	/**
	 * Convert the given observable instance into a classical iterable instance.
	 * @param <T> the element type to iterate
	 * @param observable the original observable
	 * @param pool the pool where to await elements from the observable.
	 * @return the iterable
	 */
	@Nonnull 
	public static <T> Iterable<T> asIterable(
			@Nonnull final Observable<? extends T> observable, 
			@Nonnull final Scheduler pool) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
				
				final Closeable c = observable.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						queue.add(Option.<T>error(ex));
					}

					@Override
					public void finish() {
						queue.add(Option.<T>none());
					}

					@Override
					public void next(T value) {
						queue.add(Option.some(value));
					}
					
				});
				
				return new Iterator<T>() {
					/** Close the association if there is no more elements. */
					Closeable close = c;
					/** The peek value due hasNext. */
					Option<T> peek;
					/** Indicator if there was a hasNext() call before the next() call. */
					boolean peekBeforeNext;
					/** Close the helper observer. */
					void close() {
						if (close != null) {
							try {
								close.close();
								close = null;
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					}
					@Override
					protected void finalize() throws Throwable {
						close();
					}
					@Override
					public boolean hasNext() {
						if (peek != Option.none()) {
							if (!peekBeforeNext) {
								try {
									peek = queue.take();
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
							}
							peekBeforeNext = true;
						}
						boolean result = peek != Option.none();
						if (!result) {
							close();
						}
						return result;
					}
					@Override
					public T next() {
						if (peekBeforeNext) {
							peekBeforeNext = false;
							if (peek != Option.none()) {
								return peek.value();
							}
							close();
							throw new NoSuchElementException();
						}
						peekBeforeNext = false;
						if (peek != Option.none()) {
							try {
								peek = queue.take();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							if (peek != Option.none()) {
								return peek.value();
							}
						}
						close();
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
	 * Convert the functional observable into a normal observable object.
	 * @param <T> the type of the elements to observe.
	 * @param source the source of the functional-observable elements
	 * @return the observable object
	 */
	@Nonnull 
	public static <T> Observable<T> asObservable(
			@Nonnull final Action1<Action1<Option<T>>> source) {
		return Reactive.create(new Func1<Action0, Observer<? super T>>() {
			@Override
			public Action0 invoke(final Observer<? super T> o) {
				source.invoke(asAction(o));
				return Actions.noAction0();
			}
		});
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * default pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @return the observable 
	 */
	@Nonnull 
	public static <T> Observable<T> asObservable(
			@Nonnull final Iterable<? extends T> iterable) {
		return asObservable(iterable, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * given pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @param pool the thread pool where to generate the events from the iterable
	 * @return the observable 
	 */
	@Nonnull 
	public static <T> Observable<T> asObservable(
			@Nonnull final Iterable<? extends T> iterable, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (T t : iterable) {
							if (cancelled()) {
								break;
							}
							observer.next(t);
						}
						
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}	
	/**
	 * Transform the given action to an observer.
	 * The wrapper observer converts its next() messages to Option.some(),
	 * the finish() to Option.none() and error() to Option.error().
	 * @param <T> the element type to observe
	 * @param action the action to wrap
	 * @return the observer
	 */
	@Nonnull 
	public static <T> Observer<T> asObserver(
			@Nonnull final Action1<? super Option<T>> action) {
		return new Observer<T>() {
			@Override
			public void error(Throwable ex) {
				action.invoke(Option.<T>error(ex));
			}

			@Override
			public void finish() {
				action.invoke(Option.<T>none());
			}

			@Override
			public void next(T value) {
				action.invoke(Option.some(value));
			}
			
		};
	}
	/**
	 * Computes and signals the average value of the BigDecimal source.
	 * The source may not send nulls.
	 * @param source the source of BigDecimals to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<BigDecimal> averageBigDecimal(
			@Nonnull final Observable<BigDecimal> source) {
		return aggregate(source, 
			Functions.sumBigDecimal(),
			new Func2<BigDecimal, BigDecimal, Integer>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, Integer param2) {
					return param1.divide(BigDecimal.valueOf(param2.longValue()), RoundingMode.HALF_UP);
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the BigInteger source.
	 * The source may not send nulls.
	 * @param source the source of BigIntegers to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<BigDecimal> averageBigInteger(
			@Nonnull final Observable<BigInteger> source) {
		return aggregate(source, 
			Functions.sumBigInteger(),
			new Func2<BigDecimal, BigInteger, Integer>() {
				@Override
				public BigDecimal invoke(BigInteger param1, Integer param2) {
					return new BigDecimal(param1).divide(BigDecimal.valueOf(param2.longValue()), RoundingMode.HALF_UP);
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the Double source.
	 * The source may not send nulls.
	 * @param source the source of Doubles to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<Double> averageDouble(
			@Nonnull final Observable<Double> source) {
		return aggregate(source, 
			Functions.sumDouble(),
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the Float source.
	 * The source may not send nulls.
	 * @param source the source of Floats to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<Float> averageFloat(
			@Nonnull final Observable<Float> source) {
		return aggregate(source, 
			Functions.sumFloat(),
			new Func2<Float, Float, Integer>() {
				@Override
				public Float invoke(Float param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the integer source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @param source the source of integers to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<Double> averageInt(
			@Nonnull final Observable<Integer> source) {
		return aggregate(source, 
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					if (param1 != null) {
						return param1 + param2;
					}
					return param2.doubleValue();
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
	 * Computes and signals the average value of the Long source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public static Observable<Double> averageLong(
			@Nonnull final Observable<Long> source) {
		return aggregate(source, 
			new Func2<Double, Double, Long>() {
				@Override
				public Double invoke(Double param1, Long param2) {
					if (param1 != null) {
						return param1 + param2;
					}
					return param2.doubleValue();
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
	 * Buffer the nodes as they become available and send them out in bufferSize chunks.
	 * The observers return a new and modifiable list of T on every next() call.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param bufferSize the target buffer size
	 * @return the observable of the list
	 */
	@Nonnull 
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source, 
			final int bufferSize) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current buffer. */
					List<T> buffer;

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (buffer != null && buffer.size() > 0) {
							observer.next(buffer);
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (buffer == null) {
							buffer = new ArrayList<T>(bufferSize);
						}
						buffer.add(value);
						if (buffer.size() == bufferSize) {
							observer.next(buffer);
							buffer = new ArrayList<T>(bufferSize);
						}
					}
					
				});
			}
		};
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * It uses the default scheduler pool.
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait betveen buffer fills
	 * @param unit the time unit
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source, 
			final int bufferSize, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return buffer(source, bufferSize, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait betveen buffer fills
	 * @param unit the time unit
	 * @param pool the pool where to schedule the buffer splits
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source, 
			final int bufferSize, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final AtomicInteger bufferLength = new AtomicInteger();
				final Lock lock = new ReentrantLock(true);
				final DefaultRunnable r = new DefaultRunnable(lock) {
					@Override
					public void onRun() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());
						observer.next(curr);
					}
				};
				DefaultObserver<T> s = new DefaultObserver<T>(lock, true) {
					/** The timer companion. */
					Closeable timer = pool.schedule(r, unit.toNanos(time), unit.toNanos(time));
					@Override
					protected void onClose() {
						close0(timer);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void onFinish() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());
						observer.next(curr);
						
						observer.finish();
					}
					
					/** The buffer to fill in. */
					@Override
					public void onNext(T value) {
						buffer.add(value);
						if (bufferLength.incrementAndGet() == bufferSize) {
							List<T> curr = new ArrayList<T>();
							buffer.drainTo(curr);
							bufferLength.addAndGet(-curr.size());
							
							observer.next(curr);
						}
					}
				};
				return close(s, source.register(s));
			}
		};
		
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * The scheduling is done on the default Scheduler.
	 * @param <T> the type of elements to observe
	 * @param source the source of Ts.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return buffer(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * @param <T> the type of elements to observe
	 * @param source the source of Ts.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @param pool the scheduled execution pool to use
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				
				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final Lock lock = new ReentrantLock(true);
				
				final DefaultRunnable r = new DefaultRunnable(lock) {
					@Override
					public void onRun() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
					}
				};
				DefaultObserver<T> o = new DefaultObserver<T>(lock, true) {
					Closeable timer = pool.schedule(r, unit.toNanos(time), unit.toNanos(time));
					@Override
					protected void onClose() {
						close0(timer);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void onFinish() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
						observer.finish();
					}
					/** The buffer to fill in. */
					@Override
					public void onNext(T value) {
						buffer.add(value);
					}
				};
				return close(o, source.register(o));
			}
		};
	}
	/**
	 * Wraps two or more closeables into one closeable.
	 * <code>IOException</code>s thrown from the closeables are suppressed.
	 * @param c0 the first closeable
	 * @param c1 the second closeable
	 * @param closeables the rest of the closeables
	 * @return the composite closeable
	 */
	@Nonnull 
	static Closeable close(
			@Nonnull final Closeable c0, 
			@Nonnull final Closeable c1, 
			@Nonnull final Closeable... closeables) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				try {
					c0.close();
				} catch (IOException ex) {
					
				}
				try {
					c1.close();
				} catch (IOException ex) {
					
				}
				for (Closeable c : closeables) {
					try {
						c.close();
					} catch (IOException ex) {
						
					}
				}
			}
		};
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
	 * Creates a composite closeable from the array of closeables.
	 * <code>IOException</code>s thrown from the closeables are suppressed.
	 * @param closeables the closeables array
	 * @return the composite closeable
	 */
	@Nonnull 
	static Closeable closeAll(
			@Nonnull final Iterable<? extends Closeable> closeables) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				for (Closeable c : closeables) {
					close0(c);
				}
			}
		};
	}
	/**
	 * Concatenates the source observables in a way that when the first finish(), the
	 * second gets registered and continued, and so on.
	 * FIXME not sure how it should handle closability
	 * @param <T> the type of the values to observe
	 * @param sources the source list of subsequent observables
	 * @return the concatenated observable
	 */
	@Nonnull 
	public static <T> Observable<T> concat(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserver<T> obs = new DefaultObserver<T>(false) {
						/** The current registration. */
						@GuardedBy("lock")
						Closeable current;
						{
							lock.lock();
							try {
								current = it.next().register(this);
							} finally {
								lock.unlock();
							}
						}
						@Override
						protected void onClose() {
							close0(current);
						}
	
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							close();
						}
	
						@Override
						public void onFinish() {
							if (it.hasNext()) {
								close0(current);
								current = it.next().register(this);
							} else {
								observer.finish();
								close();
							}
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					return obs;
				}
				return Reactive.<T>empty().register(observer);
			}
		};
	}
	/**
	 * Concatenate two observables in a way when the first finish() the second is registered
	 * and continued with.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the concatenated observable
	 */
	@Nonnull 
	public static <T> Observable<T> concat(
			@Nonnull Observable<? extends T> first, 
			@Nonnull Observable<? extends T> second) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>();
		list.add(first);
		list.add(second);
		return concat(list);
	}
	/**
	 * Signals a single TRUE if the source observable signals a value equals() with the source value.
	 * Both the source and the test value might be null. The signal goes after the first encounter of
	 * the given value.
	 * @param <T> the type of the observed values
	 * @param source the source observable
	 * @param value the value to look for
	 * @return the observer for contains
	 */
	@Nonnull 
	public static <T> Observable<Boolean> contains(
			@Nonnull final Observable<? extends T> source, 
			final T value) {
		return any(source, new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return param1 == value || (param1 != null && param1.equals(value));
			};
		});
	}
	/**
	 * Counts the number of elements in the observable source.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the count signal
	 */
	@Nonnull 
	public static <T> Observable<Integer> count(
			@Nonnull final Observable<T> source) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				//FIXME sequence guaranties?
				return source.register(new Observer<T>() {
					/** The counter. */
					int count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
					}

					@Override
					public void next(T value) {
						count++;
					}
					
				});
			}
		};
	}
	/**
	 * Counts the number of elements in the observable source as a long.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the count signal
	 */
	@Nonnull 
	public static <T> Observable<Long> countLong(
			@Nonnull final Observable<T> source) {
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				//FIXME sequence guaranties?
				return source.register(new Observer<T>() {
					/** The counter. */
					long count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
					}

					@Override
					public void next(T value) {
						count++;
					}
					
				});
			}
		};
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers.
	 * @param <T> the type of the value to observe
	 * @param subscribe the function to manage new subscriptions
	 * @return the observable instance
	 */
	@Nonnull 
	public static <T> Observable<T> create(
			@Nonnull final Func1<? extends Action0, Observer<? super T>> subscribe) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				final Action0 a = subscribe.invoke(observer);
				return new Closeable() {
					@Override
					public void close() {
						a.invoke();
					}
				};
			}
		};
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers and returns a custom Closeable to terminate the registration.
	 * @param <T> the type of the value to observe
	 * @param subscribe the function to manage new subscriptions
	 * @return the observable instance
	 */
	@Nonnull 
	public static <T> Observable<T> createWithCloseable(
			@Nonnull final Func1<? extends Closeable, Observer<? super T>> subscribe) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return subscribe.invoke(observer);
			}
		};
	}
	/**
	 * Constructs an observer which logs errors in case next(), finish() or error() is called 
	 * and the observer is not in running state anymore due an earlier finish() or error() call.
	 * @param <T> the element type.
	 * @param source the source observable
	 * @return the augmented observable
	 */
	@Nonnull 
	public static <T> Observable<T> debugState(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					ObserverState state = ObserverState.OBSERVER_RUNNING;
					@Override
					public void error(Throwable ex) {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						state = ObserverState.OBSERVER_ERROR;
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						state = ObserverState.OBSERVER_FINISHED;
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						observer.next(value);
					}
					
				});
			}
		};
	}
	/**
	 * The returned observable invokes the <code>observableFactory</code> whenever an observer
	 * tries to subscribe to it.
	 * @param <T> the type of elements to observer
	 * @param observableFactory the factory which is responsivle to create a source observable.
	 * @return the result observable
	 */
	@Nonnull 
	public static <T> Observable<T> defer(
			@Nonnull final Func0<? extends Observable<? extends T>> observableFactory) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return observableFactory.invoke().register(observer);
			}
		};
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifiactions.
	 * It uses the default scheduler pool when submitting the delayed values
	 * @param <T> the type of elements
	 * @param source the source of Ts
	 * @param time the time value
	 * @param unit the time unit
	 * @return the delayed observable of Ts
	 */
	@Nonnull 
	public static <T> Observable<T> delay(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return delay(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifiactions
	 * @param <T> the type of elements
	 * @param source the source of Ts
	 * @param time the time value
	 * @param unit the time unit
	 * @param pool the pool to use for scheduling
	 * @return the delayed observable of Ts
	 */
	@Nonnull 
	public static <T> Observable<T> delay(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The outstanding requests. */
					final BlockingQueue<Closeable> outstanding = new LinkedBlockingQueue<Closeable>();
					@Override
					public void onClose() {
						List<Closeable> list = new LinkedList<Closeable>();
						outstanding.drainTo(list);
						for (Closeable c : list) {
							close0(c);
						}
						super.close();
					}

					@Override
					public void onError(final Throwable ex) {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.error(ex);
									close();
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, unit.toNanos(time)));
					}

					@Override
					public void onFinish() {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.finish();
									close();
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, unit.toNanos(time)));
					}
					@Override
					public void onNext(final T value) {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.next(value);
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, unit.toNanos(time)));
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Returns an observable which converts all option messages
	 * back to regular next(), error() and finish() messages.
	 * The returned observable adheres to the <code>next* (error|finish)?</code> pattern,
	 * which ensures that no further options are relayed after an error or finish.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @see #materialize(Observable)
	 */
	@Nonnull 
	public static <T> Observable<T> dematerialize(
			@Nonnull final Observable<? extends Option<T>> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<Option<T>>() {
					/** Keeps track of the observer's state. */
					final AtomicBoolean done = new AtomicBoolean();
					@Override
					public void error(Throwable ex) {
						if (!done.get()) {
							done.set(true);
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!done.get()) {
							done.set(true);
							observer.finish();
						}
					}

					@Override
					public void next(Option<T> value) {
						if (!done.get()) {
							if (Option.isNone(value)) {
								done.set(true);
								observer.finish();
							} else
							if (Option.isSome(value)) {
								observer.next(value.value());
							} else {
								done.set(true);
								observer.error(Option.getError(value));
							}
						}
					}
					
				});
			}
		};
	}
	/**
	 * Dispatches the option to the various Observer methods.
	 * @param <T> the value type
	 * @param observer the observer
	 * @param value the value to dispatch
	 */
	@Nonnull 
	public static <T> void dispatch(
			@Nonnull Observer<? super T> observer, 
			@Nonnull Option<T> value) {
		if (value == Option.none()) {
			observer.finish();
		} else
		if (Option.isError(value)) {
			observer.error(((Option.Error<?>)value).error());
		} else {
			observer.next(value.value());
		}
	}
	/**
	 * Returns an observable which fires next() events only when the subsequent values differ
	 * in terms of Object.equals().
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source) {
		return distinct(source, Functions.<T>identity());
	}
	/**
	 * Returns Ts from the source observable if the subsequent keys extracted by <code>keyExtractor</code> are different.
	 * @param <T> the type of the values to observe
	 * @param <U> the key type check for distinction
	 * @param source the source of Ts
	 * @param keyExtractor the etractor for the keys
	 * @return the new filtered observable
	 */
	@Nonnull 
	public static <T, U> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<U, T> keyExtractor) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Indication as the first. */
					boolean first = true;
					/** The last value. */
					U lastKey;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						U key = keyExtractor.invoke(value);
						if (first) {
							first = false;
							observer.next(value);
						} else
						if (lastKey != value && (lastKey == null || !lastKey.equals(key))) {
							observer.next(value);
						}
						lastKey = key;
					}
					
				});
			}
		};
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump. Uses the default pool.
	 * FIXME not sure what this method should do and how.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param pump the pump that drains the queue
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<Void> drain(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Observable<Void>, ? super T> pump) {
		return drain(source, pump, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump.
	 * FIXME not sure what this method should do and how.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param pump the pump that drains the queue
	 * @param pool the pool for the drain
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<Void> drain(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Observable<Void>, ? super T> pump, 
			@Nonnull final Scheduler pool) {
		return new Observable<Void>() {
			@Override
			public Closeable register(final Observer<? super Void> observer) {
				// keep track of the forked observers so the last should invoke finish() on the observer
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The work in progress counter. */
					final AtomicInteger wip = new AtomicInteger(1);
					/** The executor which ensures the sequence. */
					final SingleLaneExecutor<T> exec = new SingleLaneExecutor<T>(pool, new Action1<T>() {
						@Override
						public void invoke(T value) {
							pump.invoke(value).register(
								new Observer<Void>() {
									@Override
									public void error(Throwable ex) {
										lock.lock();
										try {
											observer.error(ex);
											close();
										} finally {
											lock.unlock();
										}
									}

									@Override
									public void finish() {
										if (wip.decrementAndGet() == 0) {
											observer.finish();
											close();
										}
									}

									@Override
									public void next(Void value) {
										throw new AssertionError();
									}
									
								}
							);
						};
					});
					@Override
					public void onClose() {
//						exec.close(); FIXME should not cancel the pool?!
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					public void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
					}
					@Override
					public void onNext(T value) {
						wip.incrementAndGet();
						exec.add(value);
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * @param <T> the type of the values to observe (irrelevant)
	 * @return Returns an empty observable which signals only finish() on the default observer pool.
	 */
	@Nonnull 
	public static <T> Observable<T> empty() {
		return empty(DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns an empty observable which signals only finish() on the given pool.
	 * @param <T> the expected type, (irrelevant)
	 * @param pool the pool to invoke the the finish()
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> empty(
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.finish();
					}
				});
			}
		};
	}
	/**
	 * Invokes the given action when the source signals a finish() or error().
	 * @param <T> the type of the observed values
	 * @param source the source of Ts
	 * @param action the action to invoke on finish() or error()
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> finish(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Action0 action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						action.invoke();
						observer.error(ex);
					}

					@Override
					public void finish() {
						action.invoke();
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(value);
					}
					
				});
			}
		};
	}
	/**
	 * Blocks until the first element of the observable becomes availabel and returns that element.
	 * Might block forever.
	 * Might throw a NoSuchElementException when the observable doesn't produce any more elements
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @return the first element
	 */
	public static <T> T first(
			@Nonnull final Observable<T> source) {
		Iterator<T> it = asIterable(source).iterator();
		if (it.hasNext()) {
			return it.next();
		}
		throw new NoSuchElementException();
	}
	/**
	 * Creates a concatenated sequence of Observables based on the decision function of <code>selector</code> keyed by the source iterable.
	 * @param <T> the type of the source values
	 * @param <U> the type of the observable elements.
	 * @param source the source of keys
	 * @param selector the selector of keys which returns a new observable
	 * @return the concatenated observable.
	 */
	public static <T, U> Observable<U> forEach(
			@Nonnull final Iterable<? extends T> source, 
			@Nonnull final Func1<? extends Observable<? extends U>, ? super T> selector) {
		List<Observable<? extends U>> list = new ArrayList<Observable<? extends U>>();
		for (T t : source) {
			list.add(selector.invoke(t));
		}
		return concat(list);
	}
	/**
	 * Runs the observables in parallel and joins their last values whenever one fires.
	 * FIXME not sure what this method should do in case of error.
	 * @param <T> the type of the source values
	 * @param sources the list of sources
	 * @return the observable 
	 */
	@Nonnull 
	public static <T> Observable<List<T>> forkJoin(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final List<AtomicReference<T>> lastValues = new ArrayList<AtomicReference<T>>();
				final List<Observable<? extends T>> observableList = new ArrayList<Observable<? extends T>>();
				final List<Observer<T>> observers = new ArrayList<Observer<T>>();
				final AtomicInteger wip = new AtomicInteger(observableList.size() + 1);
				
				int i = 0;
				for (Observable<? extends T> o : sources) {
					final int j = i;
					observableList.add(o);
					lastValues.add(new AtomicReference<T>());
					observers.add(new Observer<T>() {
						/** The last value. */
						T last;
						@Override
						public void error(Throwable ex) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void finish() {
							lastValues.get(j).set(last);
							runIfComplete(observer, lastValues, wip);
						}

						@Override
						public void next(T value) {
							last = value;
						}
						
					});
				}
				List<Closeable> closeables = new ArrayList<Closeable>();
				i = 0;
				for (Observable<? extends T> o : observableList) {
					closeables.add(o.register(observers.get(i)));
					i++;
				}
				runIfComplete(observer, lastValues, wip);
				return closeAll(closeables);
			}
			/**
			 * Runs the completion sequence once the WIP drops to zero.
			 * @param observer the observer who will receive the values
			 * @param lastValues the array of last values
			 * @param wip the work in progress counter
			 */
			public void runIfComplete(
					final Observer<? super List<T>> observer,
					final List<AtomicReference<T>> lastValues,
					final AtomicInteger wip) {
				if (wip.decrementAndGet() == 0) {
					List<T> values = new ArrayList<T>();
					for (AtomicReference<T> r : lastValues) {
						values.add(r.get());
					}
					observer.next(values);
					observer.finish();
				}
			}
		};
	}
	/**
	 * Generates a stream of Us by using a value T stream using the default pool fo the generator loop.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U> Observable<U> generate(
			final T initial, 
			@Nonnull final Func1<Boolean, ? super T> condition, 
			@Nonnull final Func1<? extends T, ? super T> next, 
			@Nonnull final Func1<? extends U, ? super T> selector) {
		return generate(initial, condition, next, selector, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param pool the thread pool where the generation loop should run.
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U> Observable<U> generate(
			final T initial, 
			@Nonnull final Func1<Boolean, ? super T> condition, 
			@Nonnull final Func1<? extends T, ? super T> next, 
			@Nonnull final Func1<? extends U, ? super T> selector, 
			@Nonnull final Scheduler pool) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						T t = initial;
						while (condition.invoke(t) && !cancelled()) {
							observer.next(selector.invoke(t));
							t = next.invoke(t);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param delay the selector which tells how much to wait before releasing the next U
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U> Observable<Timestamped<U>> generateTimed(
			final T initial, 
			@Nonnull final Func1<Boolean, ? super T> condition, 
			@Nonnull final Func1<? extends T, ? super T> next, 
			@Nonnull final Func1<? extends U, ? super T> selector, 
			@Nonnull final Func1<Long, ? super T> delay) {
		return generateTimed(initial, condition, next, selector, delay, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param delay the selector which tells how much to wait (in milliseconds) before releasing the next U
	 * @param pool the scheduled pool where the generation loop should run.
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U> Observable<Timestamped<U>> generateTimed(
			final T initial, 
			@Nonnull final Func1<Boolean, ? super T> condition, 
			@Nonnull final Func1<? extends T, ? super T> next, 
			@Nonnull final Func1<? extends U, ? super T> selector, 
			@Nonnull final Func1<Long, ? super T> delay, 
			@Nonnull final Scheduler pool) {
		return new Observable<Timestamped<U>>() {
			@Override
			public Closeable register(final Observer<? super Timestamped<U>> observer) {
				// the cancellation indicator
				
				DefaultRunnable s = new DefaultRunnable() {
					T current = initial;
					@Override
					public void onRun() {
						U invoke = selector.invoke(current);
						Timestamped<U> of = Timestamped.of(invoke, System.currentTimeMillis());
						observer.next(of);
						final T tn = next.invoke(current);
						current = tn;
						if (condition.invoke(tn) && !cancelled()) {
							pool.schedule(this, TimeUnit.MILLISECONDS.toNanos(delay.invoke(tn)));
						} else {
							if (!cancelled()) {
								observer.finish();
							}
						}
						
					}
				};
				
				if (condition.invoke(initial)) {
					return pool.schedule(s, TimeUnit.MILLISECONDS.toNanos(delay.invoke(initial)));
				}
				return Functions.EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * @return the current default pool used by the Observables methods
	 */
	@Nonnull 
	public static Scheduler getDefaultScheduler() {
		return DEFAULT_SCHEDULER.get();
	}
	/**
	 * Group the specified source accoring to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * FIXME not sure how this should work.
	 * @param <T> the type of the source element
	 * @param <Key> the key type of the group
	 * @param source the source of Ts
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @return the observable
	 */
	@Nonnull 
	public static <T, Key> Observable<GroupedObservable<Key, T>> groupBy(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor) {
		return groupBy(source, keyExtractor, Functions.<T>identity());
	}
	/**
	 * Group the specified source accoring to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * FIXME not sure how this should work
	 * @param <T> the type of the source element
	 * @param <U> the type of the output element
	 * @param <Key> the key type of the group
	 * @param source the source of Ts
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @param valueExtractor the extractor which makes Us from Ts
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U, Key> Observable<GroupedObservable<Key, U>> groupBy(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor, 
			@Nonnull final Func1<? extends U, ? super T> valueExtractor) {
		return new Observable<GroupedObservable<Key, U>>() {
			@Override
			public Closeable register(
					final Observer<? super GroupedObservable<Key, U>> observer) {
				final ConcurrentMap<Key, GroupedRegisteringObservable<Key, U>> knownGroups = new ConcurrentHashMap<Key, GroupedRegisteringObservable<Key, U>>();
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						for (GroupedRegisteringObservable<Key, U> group : knownGroups.values()) {
							group.finish();
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						final Key key = keyExtractor.invoke(value);
						GroupedRegisteringObservable<Key, U> group = knownGroups.get(key);
						if (group == null) {
							group = new GroupedRegisteringObservable<Key, U>(key);
							GroupedRegisteringObservable<Key, U> group2 = knownGroups.putIfAbsent(key, group);
							if (group2 != null) {
								group = group2;
							}
							observer.next(group);
						}
						group.next(valueExtractor.invoke(value));
					}
					
				});
			}
		};
	}
	/**
	 * Returns an observable where the submitted condition decides whether the <code>then</code> source is allowed to submit values.
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition, 
			@Nonnull final Observable<? extends T> then) {
		return ifThen(condition, then, Reactive.<T>never());
	}
	/**
	 * Returns an observable where the submitted condition decides whether the <code>then</code> or <code>orElse</code> 
	 * source is allowed to submit values.
	 * FIXME not sure how it should work
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @param orElse the source to use when the condition is false
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition, 
			@Nonnull final Observable<? extends T> then, 
			@Nonnull final Observable<? extends T> orElse) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Closeable s1 = then.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						if (condition.invoke()) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (condition.invoke()) {
							observer.finish();
						}
					}

					@Override
					public void next(T value) {
						if (condition.invoke()) {
							observer.next(value);
						}
					}
					
				});
				final Closeable s2 = orElse.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						if (!condition.invoke()) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!condition.invoke()) {
							observer.finish();
						}
					}

					@Override
					public void next(T value) {
						if (!condition.invoke()) {
							observer.next(value);
						}
					}
					
				});
				
				return close(s1, s2);
			}
		};
	}
	/**
	 * Invoke a specific action before relaying the Ts to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param <T> the type of the values observed
	 * @param source the source of Ts
	 * @param action the action to invoke on every T
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> invoke(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Action1<? super T> action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						action.invoke(value);
						observer.next(value);
					}
					
				});
			}
		};
	}
	/**
	 * Invoke a specific observer before relaying the Ts, finish() and error() to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param <T> the type of the values observed
	 * @param source the source of Ts
	 * @param observer the observer to invoke before any registered observers are called
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> invoke(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Observer<? super T> observer) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> o) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
						o.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
						o.finish();
					}

					@Override
					public void next(T value) {
						observer.next(value);
						o.next(value);
					}
					
				});
			}
		};
	}
	/**
	 * Invoke the given callable on the default pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Callable<? extends T> call) {
		return invokeAsync(call, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @param pool the thread pool
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Callable<? extends T> call, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							observer.next(call.call());
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run) {
		return invokeAsync(run, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param defaultValue the value to return when the Runnable completes
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run,
			final T defaultValue) {
		return invokeAsync(run, defaultValue, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param pool the thread pool
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							run.run();
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param pool the thread pool
	 * @param defaultValue the value to return by default
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run, 
			final T defaultValue,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							run.run();
							observer.next(defaultValue);
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Signals true if the source observable fires finish() without ever firing next().
	 * This means once the next() is fired, the resulting observer will return early.
	 * @param source the source observable of any type
	 * @return the observer
	 */
	@Nonnull
	public static Observable<Boolean> isEmpty(
			@Nonnull final Observable<?> source) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<Object>() {
					/** We already determined the answer? */
					boolean done;
					@Override
					public void error(Throwable ex) {
						if (!done) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!done) {
							done = true;
							observer.next(false);
							observer.finish();
						}
					}

					@Override
					public void next(Object value) {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					}
					
				});
			}
		};
	}
	/**
	 * Returns the last element of the source observable or throws
	 * NoSuchElementException if the source is empty.
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @return the last element
	 */
	@Nonnull
	public static <T> T last(
			@Nonnull final Observable<? extends T> source) {
		final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
		Closeable c = source.register(new Observer<T>() {
			/** The current value. */
			T current;
			/** Are we the first? */
			boolean first = true;
			@Override
			public void error(Throwable ex) {
				queue.add(Option.<T>none());
			}

			@Override
			public void finish() {
				if (first) {
					queue.add(Option.<T>none());
				} else {
					queue.add(Option.some(current));
				}
			}

			@Override
			public void next(T value) {
				first = false;
				current = value;
			}
			
		});
		try {
			Option<T> value = queue.take();
			c.close();
			if (value == Option.none()) {
				throw new NoSuchElementException();
			}
			return value.value();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	/**
	 * Returns an iterable which returns values on a momentary basis from the
	 * source. Useful when source produces values at different rate than the consumer takes it.
	 * The iterable.next() call might block until the first value becomes available or something else happens in the observable
	 * FIXME not sure where the observer should run
	 * @param <T> the type of the values
	 * @param source the source
	 * @return the iterable
	 */
	@Nonnull
	public static <T> Iterable<T> latest(
			@Nonnull final Observable<? extends T> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final AtomicBoolean complete = new AtomicBoolean();
				final CountDownLatch first = new CountDownLatch(1);
				final AtomicBoolean hasValue = new AtomicBoolean();
				final AtomicReference<T> current = new AtomicReference<T>();
				final Closeable c = source.register(new Observer<T>() {
					/** Set the has value once. */
					boolean once = true;
					@Override
					public void error(Throwable ex) {
						complete.set(true);
						first.countDown();
					}

					@Override
					public void finish() {
						complete.set(true);
						first.countDown();
					}

					@Override
					public void next(T value) {
						if (once) {
							once = false;
							hasValue.set(true);
						}
						current.set(value);
						first.countDown();
					}
					
				});
				return new Iterator<T>() {
					@Override
					protected void finalize() throws Throwable {
						c.close();
					}

					@Override
					public boolean hasNext() {
						try {
							first.await();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						return !complete.get() && hasValue.get();
					}

					@Override
					public T next() {
						if (hasValue.get()) {
							return current.get();
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
	 * Returns an observable which converts all messages to an <code>Option</code> value.
	 * The returned observable does not itself signal error or finish.
	 * Its dual is the <code>dematerialize</code> method.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @see #dematerialize(Observable)
	 */
	@Nonnull 
	public static <T> Observable<Option<T>> materialize(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<Option<T>>() {
			@Override
			public Closeable register(final Observer<? super Option<T>> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.next(Option.<T>error(ex));
					}

					@Override
					public void finish() {
						observer.next(Option.<T>none());
					}

					@Override
					public void next(T value) {
						observer.next(Option.some(value));
					}
					
				});
			}
		};
	}
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the maximum value
	 */
	@Nonnull 
	public static <T extends Comparable<? super T>> Observable<T> max(
			@Nonnull final Observable<? extends T> source) {
		return aggregate(source, Functions.<T>max(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the maximum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public static <T> Observable<T> max(
			@Nonnull final Observable<T> source, 
			@Nonnull final Comparator<T> comparator) {
		return aggregate(source, Functions.<T>max(comparator), Functions.<T, Integer>identityFirst());
		}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type, which must be comparable to itself
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull 
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> maxBy(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor) {
		return minMax(source, keyExtractor, Functions.<Key>comparator(), true);
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull 
	public static <T, Key> Observable<List<T>> maxBy(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor, 
			@Nonnull final Comparator<? super Key> keyComparator) {
		return minMax(source, keyExtractor, keyComparator, true);
	}
	/**
	 * Combines the notifications of all sources. The resulting stream of Ts might come from any of the sources.
	 * @param <T> the type of the values
	 * @param sources the list of sources
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> merge(
			@Nonnull final Iterable<Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final List<Closeable> disposables = new ArrayList<Closeable>();
				List<Observable<? extends T>> sourcesList = new ArrayList<Observable<? extends T>>();
				for (Observable<? extends T> os : sources) {
					sourcesList.add(os);
				}				
				final AtomicInteger wip = new AtomicInteger(sourcesList.size() + 1);
				final List<DefaultObserver<T>> observers = new ArrayList<DefaultObserver<T>>();
				final Lock lock = new ReentrantLock();
				int i = 0;
				for (i = 0; i < sourcesList.size(); i++) {
					final int j = i++;
					DefaultObserver<T> obs = new DefaultObserver<T>(lock, true) {
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							for (int k = 0; k < observers.size(); k++) {
								if (k != j) {
									observers.get(k).close();
								}
							}
						}

						@Override
						public void onFinish() {
							if (wip.decrementAndGet() == 0) {
								observer.finish();
							}
						}

						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					observers.add(obs);
					disposables.add(observers.get(i));
				}
				for (i = 0; i < observers.size(); i++) {
					disposables.add(sourcesList.get(i).register(observers.get(i)));
				}
				if (wip.decrementAndGet() == 0) {
					observer.finish();
				}
				return closeAll(disposables);
			}
		};
	};
	/**
	 * Merge the events of two observable sequences.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the merged observable
	 */
	@Nonnull 
	public static <T> Observable<T> merge(
			@Nonnull Observable<? extends T> first, 
			@Nonnull Observable<? extends T> second) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>();
		list.add(first);
		list.add(second);
		return merge(list);
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the minimum value
	 */
	@Nonnull 
	public static <T extends Comparable<? super T>> Observable<T> min(
			@Nonnull final Observable<? extends T> source) {
		return aggregate(source, Functions.<T>min(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the minimum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public static <T> Observable<T> min(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>min(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type, which must be comparable to itself
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the minimum keyed Ts
	 */
	@Nonnull 
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> minBy(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor) {
		return minMax(source, keyExtractor, Functions.<Key>comparator(), false);
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the minimum keyed Ts
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public static <T, Key> Observable<List<T>> minBy(
			@Nonnull final Observable<T> source, 
			@Nonnull final Func1<Key, T> keyExtractor, 
			@Nonnull final Comparator<Key> keyComparator) {
		return minMax(source, keyExtractor, keyComparator, false);
	};
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @param max compute the maximums?
	 * @return the observable for the maximum keyed Ts
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public static <T, Key> Observable<List<T>> minMax(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Key, ? super T> keyExtractor, 
			@Nonnull final Comparator<? super Key> keyComparator,
			@Nonnull final boolean max
	) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key maxKey;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (collect != null) {
							observer.next(collect);
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						Key key = keyExtractor.invoke(value);
						if (collect == null) {
							maxKey = key;
							collect = new ArrayList<T>();
							collect.add(value);
						} else {
							int order = keyComparator.compare(maxKey, key);
							if (order == 0) {
								collect.add(value);
							} else
							if (max ^ (order > 0)) {
								maxKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}
					
				});
			}
		};
	}
	/**
	 * Returns an observable which never fires.
	 * @param <T> the type of the observable, irrelevant
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> never() {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return Functions.EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * Wrap the given observable object in a way that any of its observers receive callbacks on
	 * the given thread pool.
	 * @param <T> the type of the objects to observe
	 * @param source the original observable
	 * @param pool the target observable
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> observeOn(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					final SingleLaneExecutor<Runnable> run = new SingleLaneExecutor<Runnable>(pool,
						new Action1<Runnable>() {
							@Override
							public void invoke(Runnable value) {
								value.run();
							}
						}
					);

					@Override
					public void onClose() {
//						run.close(); FIXME we should not cancel the pool?!
						super.close();
					}

					@Override
					public void onError(final Throwable ex) {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.error(ex);
							}
						});
					}

					@Override
					public void onFinish() {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.finish();
							}
						});
					}
					
					@Override
					public void onNext(final T value) {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.next(value);
							}
						});
					}
				};
				return close(obs, source.register(obs));
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
	@Nonnull 
	public static <T> Observer<T> print() {
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
	@Nonnull 
	public static <T> Observer<T> print(
			final String separator, 
			final int maxLineLength) {
		return new Observer<T>() {
			/** Indicator for the first element. */
			boolean first = true;
			/** The current line length. */
			int len;
			@Override
			public void error(Throwable ex) {
				ex.printStackTrace();
			}
			@Override
			public void finish() {
				System.out.println();
			}
			@Override
			public void next(T value) {
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
	 * Creates an observer with debugging purposes. 
	 * It prints the submitted values to STDOUT with a line break, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @return the observer
	 */
	@Nonnull 
	public static <T> Observer<T> println() {
		return new Observer<T>() {
			@Override
			public void error(Throwable ex) {
				ex.printStackTrace();
			}
			@Override
			public void finish() {
				System.out.println();
			}
			@Override
			public void next(T value) {
				System.out.println(value);
			};
		};
	}
	/**
	 * Creates an observer with debugging purposes. 
	 * It prints the submitted values to STDOUT with a line break, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @param prefix the prefix to use when printing
	 * @return the observer
	 */
	@Nonnull 
	public static <T> Observer<T> println(final String prefix) {
		return new Observer<T>() {
			@Override
			public void error(Throwable ex) {
				System.err.print(prefix);
				ex.printStackTrace();
			}
			@Override
			public void finish() {
				System.out.print(prefix);
				System.out.println();
			}
			@Override
			public void next(T value) {
				System.out.print(prefix);
				System.out.println(value);
			};
		};
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<BigDecimal> range(
			@Nonnull final BigDecimal start, 
			final int count, 
			@Nonnull final BigDecimal step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/** 
	 * Creates an observable which generates BigDecimal numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<BigDecimal> range(
			@Nonnull final BigDecimal start, 
			final int count, 
			@Nonnull final BigDecimal step, 
			@Nonnull final Scheduler pool) {
		return new Observable<BigDecimal>() {
			@Override
			public Closeable register(final Observer<? super BigDecimal> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						BigDecimal value = start;
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(value);
							value = value.add(step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<BigInteger> range(
			@Nonnull final BigInteger start,
			@Nonnull final BigInteger count) {
		return range(start, count, DEFAULT_SCHEDULER.get());
	}
	/** 
	 * Creates an observable which generates BigInteger numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<BigInteger> range(
			@Nonnull final BigInteger start, 
			@Nonnull final BigInteger count, 
			final Scheduler pool) {
		return new Observable<BigInteger>() {
			@Override
			public Closeable register(final Observer<? super BigInteger> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						BigInteger end = start.add(count);
						for (BigInteger i = start; i.compareTo(end) < 0 
						&& !cancelled(); i = i.add(BigInteger.ONE)) {
							observer.next(i);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Double> range(
			final double start, 
			final int count, 
			final double step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which produces Double values from <code>start</code> in <code>count</code>
	 * amount and each subsequent element has a difference of <code>step</code>.
	 * @param start the starting value
	 * @param count how many values to produce
	 * @param step the incrementation amount
	 * @param pool the pool where to emit the values
	 * @return the observable of float
	 */
	public static Observable<Double> range(
			final double start, 
			final int count, 
			final double step, 
			@Nonnull final Scheduler pool) {
		return new Observable<Double>() {
			@Override
			public Closeable register(final Observer<? super Double> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(start + i * step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
		
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Float> range(
			final float start, 
			final int count, 
			final float step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which produces Float values from <code>start</code> in <code>count</code>
	 * amount and each subsequent element has a difference of <code>step</code>.
	 * @param start the starting value
	 * @param count how many values to produce
	 * @param step the incrementation amount
	 * @param pool the pool where to emit the values
	 * @return the observable of float
	 */
	@Nonnull 
	public static Observable<Float> range(
			final float start, 
			final int count, 
			final float step, 
			@Nonnull final Scheduler pool) {
		return new Observable<Float>() {
			@Override
			public Closeable register(final Observer<? super Float> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(start + i * step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
		
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Integer> range(
			final int start, 
			@Nonnull final int count) {
		return range(start, count, DEFAULT_SCHEDULER.get());
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	public static Observable<Integer> range(
			final int start, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = start; i < start + count && !cancelled(); i++) {
							observer.next(i);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Relay values of T while the given condition does not hold.
	 * Once the condition turns true the relaying stops.
	 * @param <T> the element type
	 * @param source the source of elements
	 * @param condition the condition that must be false to relay Ts
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> relayUntil(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<Boolean> condition) {
		return relayWhile(source, Functions.negate(condition));
	}
	/**
	 * Relay the stream of Ts until condition turns into false.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param condition the condition that must hold to relay Ts
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> relayWhile(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<Boolean> condition) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					@Override
					public void onError(Throwable ex) {
						if (condition.invoke()) {
							observer.error(ex);
						}
					}

					@Override
					public void onFinish() {
						if (condition.invoke()) {
							observer.finish();
						}
					}

					@Override
					public void onNext(T value) {
						if (condition.invoke()) {
							observer.next(value);
						} else {
							close();
						}
					}
					
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Unwrap the values within a timeinterval observable to its normal value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timeinterval way.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public static <T> Observable<T> removeTimeInterval(
			@Nonnull Observable<TimeInterval<T>> source) {
		return select(source, Reactive.<T>unwrapTimeInterval());
	}
	/**
	 * Unwrap the values within a timestamped observable to its normal value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public static <T> Observable<T> removeTimestamped(
			@Nonnull Observable<Timestamped<T>> source) {
		Func1<T, Timestamped<T>> f = Reactive.unwrapTimestamped();
		return select(source, f);
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the default pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func) {
		return repeat(func, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeatedly calls the given function <code>count</code> times to generate Ts
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param func the function to call to generate values
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			final int count) {
		return repeat(func, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeatedly calls the given function <code>count</code> times to generate Ts
	 * and runs on the given pool.
	 * @param <T> the element type
	 * @param func the function to call to generate values
	 * @param count the numer of times to repeat the value
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultRunnable r = new DefaultRunnable() {
					@Override
					public void onRun() {
						int i = count;
						while (!cancelled() && i-- > 0) {
							observer.next(func.invoke());
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(r);
			}
		};
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @param pool the pool where the generator loop runs
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultRunnable r = new DefaultRunnable() {
					@Override
					public void onRun() {
						while (!cancelled()) {
							observer.next(func.invoke());
						}
					}
				};
				return pool.schedule(r);
			}
		};
	}
	/**
	 * Repeat the source observable count times. Basically it creates
	 * a list of observables, all the source instance and applies
	 * the concat() operator on it.
	 * @param <T> the element type
	 * @param source the source observable
	 * @param count the number of times to repeat
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			@Nonnull Observable<? extends T> source, 
			int count) {
		if (count > 0) {
			List<Observable<? extends T>> srcs = new ArrayList<Observable<? extends T>>(count);
			for (int i = 0; i < count; i++) {
				srcs.add(source);
			}
			return concat(srcs);
		}
		return empty();
	}
	/**
	 * Creates an observable which repeates the given value indefinitely
	 * and runs on the default pool. Note that the observers must
	 * deregister to stop the infinite background loop
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(final T value) {
		return repeat(value, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeates the given value <code>count</code> times
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			final T value, 
			final int count) {
		return repeat(value, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeates the given value <code>count</code> times
	 * and runs on the given pool.
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param count the numer of times to repeat the value
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> repeat(
			final T value, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return repeat(Functions.constant0(value), count, pool);
	}
	/**
	 * Creates an observable which repeates the given value indefinitely
	 * and runs on the given pool. Note that the observers must
	 * deregister to stop the infinite background loop
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(
			final T value, 
			@Nonnull final Scheduler pool) {
		return repeat(Functions.constant0(value), pool);
	}
	/**
	 * Replace the current default scheduler with the specified  new scheduler.
	 * This method is threadsafe
	 * @param newScheduler the new scheduler
	 * @return the current scheduler
	 */
	@Nonnull 
	public static Scheduler replaceDefaultScheduler(
			@Nonnull Scheduler newScheduler) {
		if (newScheduler == null) {
			throw new IllegalArgumentException("newScheduler is null");
		}
		return DEFAULT_SCHEDULER.getAndSet(newScheduler);
	}
	/**
	 * Restore the default scheduler back to the <code>DefaultScheduler</code>
	 * used when this class was initialized.
	 */
	public static void restoreDefaultScheduler() {
		DEFAULT_SCHEDULER.set(new DefaultScheduler());
	}
	/**
	 * Returns an observable which listens to elements from a source until it signals an error()
	 * or finish() and continues with the next observable. The registration happens only when the
	 * previous observables finished in any way.
	 * FIXME not sure how to close previous registrations
	 * @param <T> the type of the elements
	 * @param sources the list of observables
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> resumeAlways(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserver<T> obs = new DefaultObserver<T>(false) {
						Closeable c;
						{
							lock.lock();
							try {
								c = it.next().register(this);
							} finally {
								lock.unlock();
							}
						}
						@Override
						protected void onClose() {
							close0(c);
						}

						@Override
						public void onError(Throwable ex) {
							close0(c);
							if (it.hasNext()) {
								c = it.next().register(this);
							} else {
								observer.finish();
								close();
							}
						}

						@Override
						public void onFinish() {
							close0(c);
							if (it.hasNext()) {
								c = it.next().register(this);
							} else {
								observer.finish();
								close();
							}
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					return obs;
				}
				return Reactive.<T>empty().register(observer);
			}
		};
	}
	/**
	 * It tries to submit the values of first observable, but when it throws an exeption,
	 * the next observable within source is used further on. Basically a failover between the Observables.
	 * If the current source finish() then the result observable calls finish().
	 * If the last of the sources calls error() the result observable calls error()
	 * FIXME not sure how to close previous registrations
	 * @param <T> the type of the values
	 * @param sources the available source observables.
	 * @return the failover observable
	 */
	@Nonnull 
	public static <T> Observable<T> resumeOnError(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserver<T> obs = new DefaultObserver<T>(false) {
						Closeable c;
						{
							lock.lock();
							try {
								c = it.next().register(this);
							} finally {
								lock.unlock();
							}
						}
						@Override
						protected void onClose() {
							close0(c);
						}

						@Override
						public void onError(Throwable ex) {
							close0(c);
							if (it.hasNext()) {
								c = it.next().register(this);
							} else {
								observer.finish();
								close();
							}
						}

						@Override
						public void onFinish() {
							close0(c);
							observer.finish();
							close();
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					return obs;
				}
				return Reactive.<T>empty().register(observer);
			}
		};
	}
	/**
	 * Restarts the observation until the source observable terminates normally.
	 * @param <T> the type of elements
	 * @param source the source observable
	 * @return the repeating observable
	 */
	@Nonnull 
	public static <T> Observable<T> retry(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(false) {
					/** The registration. */
					Closeable c;
					{
						lock.lock();
						try {
							c = source.register(this);
						} finally {
							lock.unlock();
						}
					}
					@Override
					protected void onClose() {
						close0(c);
					}

					@Override
					public void onError(Throwable ex) {
						close0(c);
						c = source.register(this);
					}

					@Override
					public void onFinish() {
						observer.finish();
						close();
					}
					@Override
					public void onNext(T value) {
						observer.next(value);
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Restarts the observation until the source observable terminates normally or the <code>count</code> retry count was used up.
	 * FIXME if the retry count is zero and yet another error comes, what should happen? finish or this time submit the error?
	 * @param <T> the type of elements
	 * @param source the source observable
	 * @param count the retry count
	 * @return the repeating observable
	 */
	@Nonnull 
	public static <T> Observable<T> retry(
			@Nonnull final Observable<? extends T> source, 
			final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(false) {
					/** The remaining retry count. */
					int remainingCount = count;
					/** The registration. */
					Closeable c;
					{
						lock.lock();
						try {
							c = source.register(this);
						} finally {
							lock.unlock();
						}
					}
					@Override
					public void onError(Throwable ex) {
						close0(c);
						if (remainingCount-- > 0) {
							c = source.register(this);
						} else {
							observer.error(ex);
							close();
						}
					}

					@Override
					public void onFinish() {
						observer.finish();
						close();
					}

					@Override
					public void onNext(T value) {
						observer.next(value);
					}
					
				};
				return obs;
			}
		};
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are submitted to the given action.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param action the action to invoke for each value
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static <T> void run(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Action1<? super T> action) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<T>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(T value) {
				action.invoke(value);
			}
			
		});
		try {
			latch.await();
		} finally {
			close0(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error(). Events are submitted to the given observer.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param observer the observer to invoke for each event
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static <T> void run(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Observer<? super T> observer) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<T>(true) {
			@Override
			public void onError(Throwable ex) {
				try {
					observer.error(ex);
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void onFinish() {
				try {
					observer.finish();
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void onNext(T value) {
				observer.next(value);
			}
			
		});
		try {
			latch.await();
		} finally {
			close0(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are ignored.
	 * @param source the source observable
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static void run(
			@Nonnull final Observable<?> source) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<Object>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(Object value) {
				
			}
			
		});
		try {
			latch.await();
		} finally {
			close0(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error() or the specified amount of time ellapses. Values are ignored.
	 * FIXME might be infeasible due the potential side effects along the event stream
	 * @param source the source observable
	 * @param time the time value
	 * @param unit the time unit
	 * @return false if the waiting time ellapsed before the run completed
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	static boolean run(
			@Nonnull final Observable<?> source, 
			long time, 
			@Nonnull TimeUnit unit) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<Object>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(Object value) {
				
			}
			
		});
		try {
			return latch.await(time, unit);
		} finally {
			close0(c);
		}
	}
	/**
	 * Periodically sample the given source observable, which means tracking the last value of
	 * the observable and periodically submitting it to the output observable.
	 * FIXME the error() and finish() are instantly propagated
	 * @param <T> the type of elements to watch
	 * @param source the source of elements
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @return the sampled observable
	 */
	@Nonnull 
	public static <T> Observable<T> sample(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return sample(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Periodically sample the given source observable, which means tracking the last value of
	 * the observable and periodically submitting it to the output observable.
	 * FIXME the error() and finish() are instantly propagated
	 * @param <T> the type of elements to watch
	 * @param source the source of elements
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @param pool the scheduler pool where the periodic submission should happen.
	 * @return the sampled observable
	 */
	@Nonnull 
	public static <T> Observable<T> sample(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** Are we waiting for the first event? */
					@GuardedBy("lock")
					boolean first = true;
					/** The current value. */
					@GuardedBy("lock")
					T current;
					final Closeable c = pool.schedule(new DefaultRunnable(lock) {
						@Override
						protected void onRun() {
							if (!first) {
								observer.next(current);
							}
						}
					}, unit.toNanos(time), unit.toNanos(time));
					@Override
					protected void onClose() {
						close0(c);
					}
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						first = false;
						current = value;
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Use the mapper to transform the T source into an U source.
	 * @param <T> the type of the original observable
	 * @param <U> the type of the new observable
	 * @param source the source of Ts
	 * @param mapper the mapper from Ts to Us
	 * @return the observable on Us
	 */
	@Nonnull 
	public static <T, U> Observable<U> select(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends U, ? super T> mapper) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(mapper.invoke(value));
					}
					
				});
			}
		};
	}
	/**
	 * Transforms the elements of the source observable into Us by using a selector which receives an index indicating
	 * how many elements have been transformed this far.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source observable
	 * @param selector the selector taking an index and the current T
	 * @return the transformed observable
	 */
	public static <T, U> Observable<U> select(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func2<? extends U, ? super Integer, ? super T> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The running index. */
					int index;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(selector.invoke(index++, value));
					}
					
				});
			}
		};
	}
	/**
	 * Transform the given source of Ts into Us in a way that the 
	 * selector might return an observable ofUs for a single T.
	 * The observable is fully channelled to the output observable.
	 * FIXME not sure how to do it
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us 
	 * @return the 
	 */
	@Nonnull 
	public static <T, U> Observable<U> selectMany(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Observable<? extends U>, ? super T> selector) {
		return selectMany(source, selector, new Func2<U, T, U>() {
			@Override
			public U invoke(T param1, U param2) {
				return param2;
			};
		});
	}
	/**
	 * Creates an observable in which for each of Ts an observable of Vs are
	 * requested which in turn will be transformed by the resultSelector for each
	 * pair of T and V giving an U.
	 * FIXME concurrency related questions
	 * @param <T> the source element type
	 * @param <U> the intermediate element type
	 * @param <V> the output element type
	 * @param source the source of Ts
	 * @param collectionSelector the selector which returns an observable of intermediate Vs
	 * @param resultSelector the selector which gives an U for a T and V
	 * @return the observable of Us
	 */
	@Nonnull 
	public static <T, U, V> Observable<V> selectMany(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Observable<? extends U>, ? super T> collectionSelector, 
			@Nonnull final Func2<? extends V, ? super T, ? super U> resultSelector) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(false) {
					/** The work in progress counter. */
					final AtomicInteger wip = new AtomicInteger(1);
					/** The active observers. */
					final Map<DefaultObserver<? extends U>, Closeable> active = new HashMap<DefaultObserver<? extends U>, Closeable>();
					@Override
					protected void onClose() {
						for (Closeable c : active.values()) {
							close0(c);
						}
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						close();
					}
					@Override
					public void onFinish() {
						onLast();
					}
					/**
					 * The error signal from the inner.
					 * @param ex the exception
					 */
					void onInnerError(Throwable ex) {
						onError(ex);
					}
					/** The last one will signal a finish. */
					public void onLast() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
							close();
						}
					}
					@Override
					public void onNext(final T t) {
						Observable<? extends U> sub = collectionSelector.invoke(t);
						DefaultObserver<U> o = new DefaultObserver<U>(lock, true) {
							@Override
							protected void onClose() {
								active.remove(this);
							}

							@Override
							protected void onError(Throwable ex) {
								onInnerError(ex);
								close();
							}

							@Override
							protected void onFinish() {
								onLast();
								close();
							}
							@Override
							protected void onNext(U u) {
								observer.next(resultSelector.invoke(t, u));
							}
						};
						wip.incrementAndGet();
						active.put(o, sub.register(o));
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Creates an observable of Us in a way when a source T arrives, the observable of 
	 * Us is completely drained into the output. This is done again and again for
	 * each arriving Ts.
	 * @param <T> the type of the source, irrelevant
	 * @param <U> the output type
	 * @param source the source of Ts
	 * @param provider the source of Us
	 * @return the observable for Us
	 */
	@Nonnull 
	public static <T, U> Observable<U> selectMany(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Observable<? extends U> provider) {
		return selectMany(source, Functions.<Observable<? extends U>, T>constant(provider));
	}
	/**
	 * Transform the given source of Ts into Us in a way that the selector might return zero to multiple elements of Us for a single T.
	 * The iterable is flattened and submitted to the output
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us 
	 * @return the 
	 */
	@Nonnull 
	public static <T, U> Observable<U> selectManyIterable(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? extends Iterable<? extends U>, ? super T> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						for (U u : selector.invoke(value)) {
							observer.next(u);
						}
					}
					
				});
			}
		};
	}
	/**
	 * Returns the single element of the given observable source.
	 * If the source is empty, a NoSuchElementException is thrown.
	 * If the source has more than one element, a TooManyElementsException is thrown.
	 * @param <T> the type of the element
	 * @param source the source of Ts
	 * @return the single element
	 */
	@Nonnull 
	public static <T> T single(
			@Nonnull Observable<? extends T> source) {
		Iterator<T> it = asIterable(source).iterator();
		if (it.hasNext()) {
			T one = it.next();
			if (!it.hasNext()) {
				return one;
			}
			throw new TooManyElementsException();
		}
		throw new NoSuchElementException();
	}
	/**
	 * Returns the single value in the observables.
	 * @param <T> the value type
	 * @param value the value
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> singleton(
			final T value) {
		return singleton(value, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns the single value in the observables.
	 * @param <T> the value type
	 * @param value the value
	 * @param pool the pool where to submit the value to the observers
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> singleton(
			final T value, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.next(value);
						observer.finish();
					}
				});
			}
		};
	}
	/**
	 * Skips the given amount of next() messages from source and relays
	 * the rest.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of messages to skip
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> skip(
			@Nonnull final Observable<? extends T> source, 
			final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The remaining count. */
					int remaining = count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (remaining <= 0) {
							observer.next(value);
						} else {
							remaining--;
						}
					}
				});
			}
		};
	}
	/**
	 * Skips the last <code>count</code> elements from the source observable.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to skip at the end
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> skipLast(
			@Nonnull final Observable<? extends T> source, 
			final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The temporar buffer to delay the values. */
					final CircularBuffer<T> buffer = new CircularBuffer<T>(count);
					/** The current size of the buffer. */
					int size;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						buffer.add(value);
						size++;
						if (size > count) {
							observer.next(buffer.take());
							size--;
						}
					}
					
				});
			}
		};
	}
	/**
	 * Skip the source elements until the signaller sends its first element.
	 * FIXME: If the signaller sends an error or only finish(), the relaying is never enabled?
	 * FIXME: once the singaller fires, it gets deregistered
	 * @param <T> the element type of the source
	 * @param <U> the element type of the signaller, irrelevant
	 * @param source the source of Ts
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull 
	public static <T, U> Observable<T> skipUntil(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Observable<? extends U> signaller) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The signaller observer. */
					final DefaultObserver<U> signal;
					/** The signal closeable. */
					final Closeable c;
					/** The skip gate. */
					boolean gate;
					{
						signal = new DefaultObserver<U>(lock, true) {
							@Override
							public void onError(Throwable ex) {
								innerError(ex);
							}
	
							@Override
							public void onFinish() {
								if (!gate) {
									innerFinish(); // signaller will never turn the gate on
								}
							}
	
							@Override
							public void onNext(U value) {
								gate = true;
							}
						};
						c = signaller.register(signal);
					}
					/**
					 * The callback for the inner error.
					 * @param ex the inner exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					/** The callback for an inner finish. */
					void innerFinish() {
						finish();
					}
					@Override
					protected void onClose() {
						close0(c);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						if (gate) {
							observer.next(value);
						}
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Skips the Ts from source while the specified condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed, 
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @param source the source of Ts
	 * @param condition the condition that must turn false in order to start relaying
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> skipWhile(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<Boolean, ? super T> condition) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Can we relay stuff? */
					boolean mayRelay;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (!mayRelay) {
							mayRelay = !condition.invoke(value);
							if (mayRelay) {
								observer.next(value);
							}
						} else {
							observer.next(value);
						}
					}
					
				});
			}
		};
	}
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Void> start(
			@Nonnull final Action0 action) {
		return start(action, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @param pool the pool where the action should run
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Void> start(
			@Nonnull final Action0 action, 
			@Nonnull final Scheduler pool) {
		return new Observable<Void>() {
			@Override
			public Closeable register(final Observer<? super Void> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							action.invoke();
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invokes the function asynchronously on the default pool and
	 * relays its result followed by a finish. Exceptions are
	 * relayed as well.
	 * @param <T> the function return type
	 * @param func the function
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> start(
			@Nonnull final Func0<? extends T> func) {
		return start(func, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invokes the function asynchronously on the given pool and
	 * relays its result followed by a finish. Exceptions are
	 * relayed as well.
	 * @param <T> the function return type
	 * @param func the function
	 * @param pool the pool where the action should run
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> start(
			@Nonnull final Func0<? extends T> func, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							T value = func.invoke();
							observer.next(value);
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the default pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Iterable<? extends T> values) {
		return startWith(source, values, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the given pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param values the values to start with
	 * @param pool the pool where the iterable values should be emitted
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Iterable<? extends T> values, 
			@Nonnull Scheduler pool) {
		return concat(asObservable(values, pool), source);
	}
	/**
	 * Wrap the given observable into an new Observable instance, which calls the original subscribe() method
	 * on the supplied pool. 
	 * @param <T> the type of the objects to observe
	 * @param observable the original observable
	 * @param pool the pool to perform the original subscribe() call
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> subscribeOn(
			@Nonnull final Observable<T> observable, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				// start the registration asynchronously
				final BlockingQueue<Closeable> cq = new LinkedBlockingQueue<Closeable>();
				pool.schedule(new Runnable() {
					@Override
					public void run() {
						cq.add(observable.register(observer));
					}
				}); 
				// use the disposable future when the deregistration is required
				return new Closeable() {
					@Override
					public void close() {
						pool.schedule(new Runnable() {
							@Override
							public void run() {
								try {
									cq.take().close(); // wait until the dispose becomes available then call it
								} catch (InterruptedException e) {
									throw new RuntimeException();
								} catch (IOException e) {
									throw new RuntimeException();
								}
							}
						});
					}
				};
			}
		};
	}
	/**
	 * Computes and signals the sum of the values of the BigDecimal source.
	 * The source may not send nulls.
	 * @param source the source of BigDecimals to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<BigDecimal> sumBigDecimal(
			@Nonnull final Observable<BigDecimal> source) {
		return aggregate(source, Functions.sumBigDecimal(), Functions.<BigDecimal, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the BigInteger source.
	 * The source may not send nulls.
	 * @param source the source of BigIntegers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<BigInteger> sumBigInteger(
			@Nonnull final Observable<BigInteger> source) {
		return aggregate(source, Functions.sumBigInteger(), Functions.<BigInteger, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Double source.
	 * The source may not send nulls.
	 * @param source the source of Doubles to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Double> sumDouble(
			@Nonnull final Observable<Double> source) {
		return aggregate(source, Functions.sumDouble(), Functions.<Double, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Float source.
	 * The source may not send nulls.
	 * @param source the source of Floats to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Float> sumFloat(
			@Nonnull final Observable<Float> source) {
		return aggregate(source, Functions.sumFloat(), Functions.<Float, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Integer source.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @param source the source of integers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Integer> sumInt(
			@Nonnull final Observable<Integer> source) {
		return aggregate(source, Functions.sumInteger(), Functions.<Integer, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Integer source by using
	 * a double intermediate representation.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @param source the source of integers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Double> sumIntAsDouble(
			@Nonnull final Observable<Integer> source) {
		return aggregate(source, 
			new Func2<Double, Double, Integer>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 + param2;
				}
			},
			Functions.<Double, Integer>identityFirst()
		);
	}
	/**
	 * Computes and signals the sum of the values of the Long source.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Long> sumLong(
			@Nonnull final Observable<Long> source) {
		return aggregate(source, Functions.sumLong(), Functions.<Long, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Long sourceby using
	 * a double intermediate representation.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public static Observable<Double> sumLongAsDouble(
			@Nonnull final Observable<Long> source) {
		return aggregate(source, 
				new Func2<Double, Double, Long>() {
					@Override
					public Double invoke(Double param1, Long param2) {
						return param1 + param2;
					}
				},
				Functions.<Double, Integer>identityFirst()
			);
	}
	/**
	 * Creates an observable which takes the specified number of
	 * Ts from the source, unregisters and completes.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to relay
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> take(
			@Nonnull final Observable<? extends T> source, 
			final int count) {
		return relayUntil(source, new Func0<Boolean>() {
			int i = count;
			@Override
			public Boolean invoke() {
				return i-- > 0;
			}
		});
	}
	/**
	 * Creates an observable which takes values from the source until
	 * the signaller produces a value. If the signaller never signals,
	 * all source elements are relayed.
	 * @param <T> the element type
	 * @param <U> the signaller element type, irrelevant
	 * @param source the source of Ts
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull 
	public static <T, U> Observable<T> takeUntil(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Observable<U> signaller) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The signaller closeable. */
					final Closeable c;
					{
						c = signaller.register(new DefaultObserver<U>(lock, true) {

							@Override
							protected void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							protected void onFinish() {
								innerClose();
							}

							@Override
							protected void onNext(U value) {
								close();
								innerClose();
							}
							
						});
					}
					/** Callback for the inner close. */
					void innerClose() {
						close();
					}
					/**
					 * The callback for the inner error.
					 * @param ex the inner exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onClose() {
						close0(c);
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(T value) {
						observer.next(value);
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Creates an observable which takes values from source until
	 * the predicate returns false for the current element, then skips the remaining values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param predicate the predicate
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> takeWhile(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<Boolean, ? super T> predicate) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The source closeable. */
					Closeable c;
					{
						lock.lock();
						try {
							c = source.register(this);
						} finally {
							lock.unlock();
						}
					}
					@Override
					protected void onClose() {
						close0(c);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						if (predicate.invoke(value)) {
								observer.next(value);
						} else {
							observer.finish();
							close();
						}
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing. Uses the default scheduler.
	 * @param <T> the value type 
	 * @param source the source of Ts
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> throttle(
			@Nonnull final Observable<? extends T> source, 
			final long delay, 
			@Nonnull final TimeUnit unit) {
		return throttle(source, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing.
	 * @param <T> the value type 
	 * @param source the source of Ts
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @param pool the pool where the delay-watcher should operate
	 * @return the observable
	 */
	@Nonnull 
	public static <T> Observable<T> throttle(
			@Nonnull final Observable<? extends T> source, 
			final long delay, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The last seen value. */
					T last;
					/** The closeable. */
					Closeable c;
					/** The timeout action. */
					final DefaultRunnable r = new DefaultRunnable(lock) {
						@Override
						public void onRun() {
							if (!cancelled()) {
								observer.next(last);
							}
						}
					};
					@Override
					protected void onClose() {
						close0(c);
					}
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						last = value;
						close0(c);
						c = pool.schedule(r, unit.toNanos(delay));
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Creates an observable which instantly sends the exception to
	 * its subscribers while running on the default pool.
	 * @param <T> the element type, irrelevant
	 * @param ex the exception to throw
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> throwException(
			@Nonnull final Throwable ex) {
		return throwException(ex, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which instantly sends the exception to
	 * its subscribers while running on the given pool.
	 * @param <T> the element type, irrelevant
	 * @param ex the exception to throw
	 * @param pool the pool from where to send the values
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> throwException(
			@Nonnull final Throwable ex, 
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.error(ex);
					}
				});
			}
		};
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param start the starting value of the tick
	 * @param end the finishing value of the tick exclusive
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	@Nonnull 
	public static Observable<Long> tick(
			final long start, 
			final long end, 
			final long delay, 
			@Nonnull final TimeUnit unit) {
		return tick(start, end, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * @param start the starting value of the tick inclusive
	 * @param end the finishing value of the tick exclusive
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @param pool the scheduler pool for the wait
	 * @return the observer
	 */
	@Nonnull 
	public static Observable<Long> tick(
			final long start, 
			final long end, 
			final long delay, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		if (start > end) {
			throw new IllegalArgumentException("ensure start <= end");
		}
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				return pool.schedule(new DefaultRunnable() {
					/** The current value. */
					long current = start;
					@Override
					protected void onRun() {
						if (current < end && !cancelled()) {
							observer.next(current++);
						} else {
							if (!cancelled()) {
								observer.finish();
							} else {
								cancel();
							}
						}
					}
				}, unit.toNanos(delay), unit.toNanos(delay));
			}
		};
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	@Nonnull 
	public static Observable<Long> tick(
			@Nonnull final long delay, 
			@Nonnull final TimeUnit unit) {
		return tick(0, Long.MAX_VALUE, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @return the observer.
	 */
	@Nonnull 
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return timeout(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @return the observer.
	 */
	@Nonnull 
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other) {
		return timeout(source, time, unit, other, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull 
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The current source. */
					@GuardedBy("lock")
					Closeable src;
					/** The current timer.*/
					@GuardedBy("lock")
					Closeable timer;
					{
						lock.lock();
						try {
							timer = pool.schedule(new DefaultRunnable(lock) {
								@Override
								public void onRun() {
									if (!cancelled()) {
										close0(src);
										src = other.register(observer);
										timer = null;
									}
								}
							}, unit.toNanos(time));
							src = source.register(this);
						} finally {
							lock.unlock();
						}
					}
					@Override
					protected void onClose() {
						close0(timer);
						close0(src);
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(T value) {
						if (timer != null) {
							close0(timer);
							timer = null;
						}
						observer.next(value);
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull 
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		Observable<T> other = Reactive.throwException(new TimeoutException());
		return timeout(source, time, unit, other, pool);
	}
	/**
	 * Wraps the given action as an observable which reacts only to <code>next()</code> events.
	 * @param <T> the type of the values
	 * @param action the action to wrap
	 * @return the observer wrapping the action
	 */
	@Nonnull 
	public static <T> Observer<T> toObserver(
			@Nonnull final Action1<? super T> action) {
		return new Observer<T>() {
			@Override
			public void error(Throwable ex) {
				// ignored
			}
			@Override
			public void finish() {
				// ignored
			}
			@Override
			public void next(T value) {
				action.invoke(value);
			};
		};
	}
	/**
	 * Creates an observer which calls the given functions on its similarly named methods.
	 * @param <T> the value type to receive
	 * @param next the action to invoke on next()
	 * @param error the action to invoke on error()
	 * @param finish the action to invoke on finish()
	 * @return the observer
	 */
	@Nonnull 
	public static <T> Observer<T> toObserver(
			@Nonnull final Action1<? super T> next, 
			@Nonnull final Action1<? super Throwable> error, 
			@Nonnull final Action0 finish) {
		return new Observer<T>() {
			@Override
			public void error(Throwable ex) {
				error.invoke(ex);
			}

			@Override
			public void finish() {
				finish.invoke();
			}

			@Override
			public void next(T value) {
				next.invoke(value);
			}
			
		};
	}
	/**
	 * Filters objects from source which are assignment compatible with T.
	 * Note that due java erasure complex generic types can't be filtered this way in runtime (e.g., List&lt;String>.class is just List.class).
	 * FIXME is this what cast stands for?
	 * @param <T> the type of the expected values
	 * @param source the source of unknown elements
	 * @param token the token to test agains the elements
	 * @return the observable containing Ts
	 */
	@Nonnull 
	public static <T> Observable<T> typedAs(
			@Nonnull final Observable<?> source, 
			@Nonnull final Class<T> token) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<Object>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(Object value) {
						if (token.isInstance(value)) {
							observer.next(token.cast(value));
						}
					}
					
				});
			}
		};
	}
	/**
	 * A convenience function which unwraps the T from a TimeInterval of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	@Nonnull 
	public static <T> Func1<T, TimeInterval<T>> unwrapTimeInterval() {
		return new Func1<T, TimeInterval<T>>() {
			@Override
			public T invoke(TimeInterval<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * A convenience function which unwraps the T from a Timestamped of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	@Nonnull 
	public static <T> Func1<T, Timestamped<T>> unwrapTimestamped() {
		return new Func1<T, Timestamped<T>>() {
			@Override
			public T invoke(Timestamped<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * Receives a resource from the resource selector and
	 * uses the resource until it terminates, then closes the resource.
	 * FIXME not sure how this method should work
	 * @param <T> the output resource type.
	 * @param <U> the closeable resource to work with
	 * @param resourceSelector the function that gives a resource
	 * @param resourceUsage a function that returns an observable of T for the given resource.
	 * @return the observable of Ts which terminates once the usage terminates
	 */
	@Nonnull 
	public static <T, U extends Closeable> Observable<T> using(
			@Nonnull final Func0<? extends U> resourceSelector, 
			@Nonnull final Func1<? extends Observable<? extends T>, ? super U> resourceUsage) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final U resource = resourceSelector.invoke();
				return resourceUsage.invoke(resource).register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						try {
							observer.error(ex);
						} finally {
							close0(resource);
						}
					}

					@Override
					public void finish() {
						try {
							observer.finish();
						} finally {
							close0(resource);
						}
						
					}

					@Override
					public void next(T value) {
						observer.next(value);
					}
					
				});
			}
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clause the filter clause
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<Boolean, ? super T> clause) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func2<Boolean, Integer, ? super T> clause) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current element index. */
					int index;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * The clauseFactory is used for each individual registering observer.
	 * This can be used to create memorizing filter functions such as distinct.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clauseFactory the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<Func2<Boolean, Integer, ? super T>> clauseFactory) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current element index. */
					int index;
					/** The clause factory to use. */
					final Func2<Boolean, Integer, ? super T> clause = clauseFactory.invoke();
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Splits the source stream into separate observables once
	 * the windowClosing fires an event.
	 * @param <T> the element type to observe
	 * @param <U> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowClosing the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<? extends Observable<U>> windowClosing) {
		return window(source, windowClosing, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Splits the source stream into separate observables
	 * by starting at windowOpening events and closing at windowClosing events.
	 * FIXME not sure how to implement
	 * @param <T> the element type to observe
	 * @param <U> the opening event type, irrelevant
	 * @param <V> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowOpening te source of the window opening events
	 * @param windowClosing the source of the window splitting events
	 * @param pool the pool where ???
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public static <T, U, V> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<? extends Observable<U>> windowOpening, 
			@Nonnull final Func0<? extends Observable<V>> windowClosing, 
			@Nonnull final Scheduler pool) {
		throw new UnsupportedOperationException();
	}
	/**
	 * Splits the source stream into separate observables on
	 * each windowClosing event.
	 * FIXME not sure how to implement
	 * @param <T> the element type to observe
	 * @param <U> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowClosing the source of the window splitting events
	 * @param pool the pool where the first group is signalled from directly after
	 * the registration
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<? extends Observable<U>> windowClosing, 
			@Nonnull final Scheduler pool) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				// The current observable
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The current observable window. */
					@GuardedBy("lock")
					DefaultObservable<T> current;
					/** The window watcher. */
					final DefaultObserver<U> wo = new DefaultObserver<U>(lock, true) {
						@Override
						public void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						public void onFinish() {
							innerFinish();
						}

						@Override
						public void onNext(U value) {
							DefaultObservable<T> o = new DefaultObservable<T>();
							Observer<T> os = current;
							current = o;
							if (os != null) {
								os.finish();
							}
							observer.next(o);
						}
						
					};
					/** The close handler for the inner observer of closing events. */
					Closeable woc = windowClosing.invoke().register(wo);
					/**
					 * The scheduled action which will open the first window as soon as possible.
					 */
					Closeable openWindow = pool.schedule(new DefaultRunnable(lock) {
						@Override
						protected void onRun() {
							if (current == null) {
								DefaultObservable<T> o = new DefaultObservable<T>();
								current = o;
								observer.next(o);
							}
						}
					});
					/**
					 * The inner exception callback.
					 * @param ex the exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					/** The inner finish callback. */
					void innerFinish() {
						finish();
					}
					@Override
					public void onClose() {
						close0(woc);
						close0(openWindow);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						if (current == null) {
							DefaultObservable<T> o = new DefaultObservable<T>();
							current = o;
							observer.next(o);
						}
						current.next(value);
					}
				};
				return close(obs, source.register(obs));
			}
		};
	}
	/**
	 * Wrap the given type into a timestamped container of T.
	 * @param <T> the type of the contained element
	 * @return the function performing the wrapping
	 */
	@Nonnull 
	public static <T> Func1<Timestamped<T>, T> wrapTimestamped() {
		return new Func1<Timestamped<T>, T>() {
			@Override
			public Timestamped<T> invoke(T param1) {
				return Timestamped.of(param1);
			};
		};

	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right iterable,
	 * applies the selector function and emits the resulting T.
	 * The error() and finish() signals are relayed to the output.
	 * The result is finished if the right iterator runs out of 
	 * values before the left iterator. 
	 * @param <T> the resulting element type
	 * @param <U> the value type streamed on the left observable
	 * @param <V> the value type streamed on the right iterable
	 * @param left the left observables of Us
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable 
	 */
	@Nonnull 
	public static <T, U, V> Observable<V> zip(
			@Nonnull final Observable<? extends T> left, 
			@Nonnull final Iterable<? extends U> right, 
			@Nonnull final Func2<? extends V, ? super T, ? super U> selector) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The second source. */
					final Iterator<? extends U> it = right.iterator();
					/** The registration handler. */
					final Closeable c;
					{
						lock.lock();
						try {
							c = left.register(this);
						} finally {
							lock.unlock();
						}
					}
					@Override
					protected void onClose() {
						close0(c);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T t) {
						if (it.hasNext()) {
							U u = it.next();
							observer.next(selector.invoke(t, u));
						} else {
							observer.finish();
							close();
						}
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right observable,
	 * applies the selector function and emits the resulting T.
	 * Basically it emmits a T when both an U and V is available.
	 * The output stream throws error or terminates if any of the streams 
	 * throws or terminates.
	 * FIXME not sure how to implement this, and how to close and signal
	 * @param <T> the resulting element type
	 * @param <U> the value type streamed on the left observable
	 * @param <V> the value type streamed on the right iterable
	 * @param left the left observables of Us
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable 
	 */
	@Nonnull 
	public static <T, U, V> Observable<T> zip(
			@Nonnull final Observable<? extends U> left, 
			@Nonnull final Observable<? extends V> right, 
			@Nonnull final Func2<T, U, V> selector) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final LinkedBlockingQueue<U> queueU = new LinkedBlockingQueue<U>();
				final LinkedBlockingQueue<V> queueV = new LinkedBlockingQueue<V>();
				final AtomicReference<Closeable> closeBoth = new AtomicReference<Closeable>();
				final AtomicInteger wip = new AtomicInteger(2);
				final Lock lockBoth = new ReentrantLock(true);
				
				lockBoth.lock();
				try {
					final DefaultObserver<U> oU = new DefaultObserver<U>(lockBoth, false) {
						/** The source handler. */
						final Closeable c;
						{
							lock.lock();
							try {
								c = left.register(this);
							} finally {
								lock.unlock();
							}
						}
						@Override
						protected void onClose() {
							close0(c);
						}
	
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							close0(closeBoth.get());
						}
						
						@Override
						public void onFinish() {
							if (wip.decrementAndGet() == 0) {
								observer.finish();
								close0(closeBoth.get());
							}
						}
						@Override
						public void onNext(U u) {
							V v = queueV.poll();
							if (v != null) {
								observer.next(selector.invoke(u, v));
							} else {
								if (wip.get() == 2) {
									queueU.add(u);
								} else {
									this.finish();
								}
							}
						}
					};
					final DefaultObserver<V> oV = new DefaultObserver<V>(lockBoth, false) {
						/** The source handler. */
						final Closeable c;
						{
							lock.lock();
							try {
								c = right.register(this);
							} finally {
								lock.unlock();
							}
						}
						@Override
						protected void onClose() {
							close0(c);
						}
	
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							close0(closeBoth.get());
						}
	
						@Override
						public void onFinish() {
							if (wip.decrementAndGet() == 0) {
								observer.finish();
								close0(closeBoth.get());
							}
						}
						@Override
						public void onNext(V v) {
							U u = queueU.poll();
							if (u != null) {
								observer.next(selector.invoke(u, v));
							} else {
								if (wip.get() == 2) {
									queueV.add(v);
								} else {
									this.finish();
								}
							}
						}
					};
					Closeable c = close(oU, oV);
					closeBoth.set(c);
				} finally {
					lockBoth.unlock();
				}
				return closeBoth.get();
			}
		};
	}
	/** Utility class. */
	private Reactive() {
		// utility class
	}
}
