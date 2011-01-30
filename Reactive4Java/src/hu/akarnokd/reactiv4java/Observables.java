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

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Utility class to manage observable interfaces.
 * Guidances were taken from 
 * <ul>
 * <li>http://theburningmonk.com/tags/rx/</li>
 * <li>http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx</li>
 * <li>http://rxwiki.wikidot.com/101samples#toc3</li>
 * </ul>
 * 
 * @author akarnokd
 *
 */
public final class Observables {

	/** Utility class. */
	private Observables() {
		// utility class
	}
	/** A helper disposable object which does nothing. */
	private static final Closeable EMPTY_CLOSEABLE = new Closeable() {
		@Override
		public void close() {
			
		}
	};
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers.
	 * @param <T> the type of the value to observe
	 * @param subscribe the function to manage new subscriptions
	 * @return the observable instance
	 */
	public static <T> Observable<T> create(final Func1<Action0, Observer<? super T>> subscribe) {
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
	/** The common observable pool where the Observer methods get invoked by default. */
	static final ExecutorService DEFAULT_OBSERVABLE_POOL = new ThreadPoolExecutor(0, 128, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	/** The defalt scheduler pool for delayed observable actions. */
	static final ScheduledExecutorService DEFAULT_SCHEDULED_POOL;
	static {
		DEFAULT_SCHEDULED_POOL = new ScheduledThreadPoolExecutor(1);
		((ScheduledThreadPoolExecutor)DEFAULT_SCHEDULED_POOL).setKeepAliveTime(1, TimeUnit.SECONDS);
		((ScheduledThreadPoolExecutor)DEFAULT_SCHEDULED_POOL).allowCoreThreadTimeOut(true);
	}
	/**
	 * @return the default pool used by the Observables methods by default
	 */
	public static ExecutorService getDefaultPool() {
		return DEFAULT_OBSERVABLE_POOL;
	}
	/**
	 * @return the default scheduler pool used by the Observables methods by default
	 */
	public static ScheduledExecutorService getDefaultSchedulerPool() {
		return DEFAULT_SCHEDULED_POOL;
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	public static Observable<Integer> range(final int start, final int count, final ExecutorService pool) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						for (int i = start; i < start + count && !cancel.get(); i++) {
							observer.next(i);
						}
						if (!cancel.get()) {
							observer.finish();
						}
					}
				});
				return new Closeable() {
					@Override
					public void close() {
						cancel.set(true);
					}
				};
			}
		};
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	public static Observable<Integer> range(final int start, final int count) {
		return range(start, count, DEFAULT_OBSERVABLE_POOL);
	}
	/** The wrapper for the Event dispatch thread calls. */
	private static final ExecutorService EDT_EXECUTOR =  new EdtExecutorService();
	/**
	 * Returns an executor service which executes the tasks on the event dispatch thread.
	 * @return the executor service for the EDT
	 */
	public static ExecutorService getEdtExecutor() {
		return EDT_EXECUTOR;
	}
	/**
	 * Wrap the given observable object in a way that any of its observers receive callbacks on
	 * the given thread pool.
	 * @param <T> the type of the objects to observe
	 * @param observable the original observable
	 * @param pool the target observable
	 * @return the new observable
	 */
	public static <T> Observable<T> observeOn(final Observable<T> observable, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return observable.register(new Observer<T>() {
					/** The queue to ensure the order in which the notification propagates. */
					final LinkedBlockingQueue<Runnable> inSequence = new LinkedBlockingQueue<Runnable>();
					final AtomicInteger wip = new AtomicInteger();
					@Override
					public void finish() {
						runInSequence(new Runnable() { // FIXME: not sure about sequence
							@Override
							public void run() {
								observer.finish();
							}
						});
					}
					@Override
					public void error(final Throwable ex) {
						runInSequence(new Runnable() { // FIXME: not sure about sequence
							@Override
							public void run() {
								observer.error(ex);
							}
						});
					}
					@Override
					public void next(final T value) { // FIXME: not sure about sequence
						runInSequence(new Runnable() {
							@Override
							public void run() {
								observer.next(value);
							}
						});
					};
					/**
					 * Run the specified task in sequence after
					 * any previous tasks.
					 * @param task the task to run in sequence
					 */
					private void runInSequence(final Runnable task) {
						inSequence.add(task);
						
						if (wip.incrementAndGet() == 1) {
							pool.submit(new Runnable() {
								@Override
								public void run() {
									do {
										Runnable r = inSequence.poll();
										if (r != null) {
											r.run();
										}
									} while (wip.decrementAndGet() > 0);
									 // FIXME seems to work but if a runnable blocks here, a new pool thread is started with a new instance of this
								}
							});
						}
					}
				});
			}
		};
	}
	/**
	 * Wrap the given observable into an new Observable instance, which calls the original subscribe() method
	 * on the supplied pool. 
	 * @param <T> the type of the objects to observe
	 * @param observable the original observable
	 * @param pool the pool to perform the original subscribe() call
	 * @return the new observable
	 */
	public static <T> Observable<T> subscribeOn(final Observable<T> observable, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				// start the registration asynchronously
				final Future<Closeable> future = pool.submit(new Callable<Closeable>() {
					@Override
					public Closeable call() throws Exception {
						return observable.register(observer);
					}
				}); 
				// use the disposable future when the deregistration is required
				return new Closeable() {
					@Override
					public void close() {
						pool.submit(new Runnable() {
							@Override
							public void run() {
								try {
									future.get().close(); // wait until the dispose becomes available then call it
								} catch (InterruptedException e) {
									throw new RuntimeException();
								} catch (ExecutionException e) {
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
	 * Wrap the observable to the Event Dispatch Thread for listening to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	public static <T> Observable<T> observeOnEdt(Observable<T> observable) {
		return observeOn(observable, EDT_EXECUTOR);
	}
	/**
	 * Wrap the observable to the Event Dispatch Thread for subscribing to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	public static <T> Observable<T> subscribeOnEdt(Observable<T> observable) {
		return subscribeOn(observable, EDT_EXECUTOR);
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * given pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @param pool the thread pool where to generate the events from the iterable
	 * @return the observable 
	 */
	public static <T> Observable<T> asObservable(final Iterable<T> iterable, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						for (T t : iterable) {
							observer.next(t);
						}
						observer.finish();
					}
				});
				return EMPTY_CLOSEABLE; // FIXME unsubscribe as NO-OP?
			}
		};
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * default pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @return the observable 
	 */
	public static <T> Observable<T> asObservable(final Iterable<T> iterable) {
		return asObservable(iterable, DEFAULT_OBSERVABLE_POOL);
	}	
	/**
	 * Convert the given observable instance into a classical iterable instance.
	 * FIXME how to propagte exception values?
	 * @param <T> the element type to iterate
	 * @param observable the original observable
	 * @param pool the pool where to await elements from the observable.
	 * @return the iterable
	 */
	public static <T> Iterable<T> asIterable(final Observable<T> observable, final ExecutorService pool) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
				
				final Closeable c = observable.register(new Observer<T>() {
					@Override
					public void next(T value) {
						queue.add(Option.some(value));
					}

					@Override
					public void error(Throwable ex) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void finish() {
						queue.add(Option.<T>none());
					}
					
				});
				
				return new Iterator<T>() {
					/** The peek value due hasNext. */
					Option<T> peek;
					/** Indicator if there was a hasNext() call before the next() call. */
					boolean peekBeforeNext;
					/** Close the association if there is no more elements. */
					Closeable close = c;
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
				};
			}
		};
	}
	/**
	 * Convert the given observable instance into a classical iterable instance.
	 * @param <T> the element type to iterate
	 * @param observable the original observable
	 * @return the iterable
	 */
	public static <T> Iterable<T> asIterable(final Observable<T> observable) {
		return asIterable(observable, DEFAULT_OBSERVABLE_POOL); 
	}
	/**
	 * Wraps the given action as an observable which reacts only to onNext() events.
	 * @param <T> the type of the values
	 * @param action the action to wrap
	 * @return the observer wrapping the action
	 */
	public static <T> Observer<T> asObserver(final Action1<T> action) {
		return new Observer<T>() {
			@Override
			public void finish() {
				// ignored
			}
			@Override
			public void error(Throwable ex) {
				// ignored
			}
			@Override
			public void next(T value) {
				action.invoke(value);
			};
		};
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <T> the element type
	 * @param source the source observable
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	public static <T> Observable<T> aggregate(final Observable<T> source, final Func2<T, T, T> accumulator) {
		return aggregate(source, null, accumulator);
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
	public static <T, U> Observable<U> aggregate(final Observable<T> source, final U seed, final Func2<U, U, T> accumulator) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The current aggregation result. */
					U result = seed;
					@Override
					public void next(T value) {
						result = accumulator.invoke(result, value);
					};
					@Override
					public void finish() {
						observer.next(result);
						observer.finish();
					}
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
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
	public static <T> Observable<Boolean> all(final Observable<T> source, final Func1<Boolean, T> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<T>() {
					/** Indicate if we returned early. */
					boolean done;
					@Override
					public void next(T value) {
						if (!predicate.invoke(value)) {
							done = true;
							observer.next(false);
							observer.finish();
						}
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
					public void error(Throwable ex) {
						observer.error(ex);
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
	public static <T> Observable<T> amb(final Iterable<Observable<T>> sources) {
		final AtomicReference<Observable<T>> first = new AtomicReference<Observable<T>>();
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final List<Closeable> disposers = new ArrayList<Closeable>();
				for (final Observable<T> os : sources) {
					disposers.add(os.register(new Observer<T>() {
						@Override
						public void finish() {
							Observable<T> sel = first.get();
							if (sel == os) {
								observer.finish();
							} else 
							if (sel == null) {
								if (first.compareAndSet(null, os)) {
									observer.finish();
								}
							}
						}
						@Override
						public void error(Throwable ex) {
							Observable<T> sel = first.get();
							if (sel == os) {
								observer.error(ex);
							} else 
							if (sel == null) {
								if (first.compareAndSet(null, os)) {
									observer.error(ex);
								}
							}
						}
						@Override
						public void next(T value) {
							Observable<T> sel = first.get();
							if (sel == os) {
								observer.next(value);
							} else 
							if (sel == null) {
								if (first.compareAndSet(null, os)) {
									observer.next(value);
								}
							}
						};
					}));
				}
				return new Closeable() {
					@Override
					public void close() {
						for (Closeable d : disposers) {
							try {
								d.close();
							} catch (IOException e) {
							}
						}
					}
				};
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
	public static <T> Observable<Boolean> any(final Observable<T> source) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<T>() {
					/** If we already determined the answer. */
					boolean done;
					@Override
					public void finish() {
						if (!done) {
							done = true;
							observer.next(false);
							observer.finish();
						}
					}
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void next(T value) {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					};
				});
			}
		};
	}
	/**
	 * Signals a single TRUE if the source signals any next() and the value matches the predicate before it signals a finish().
	 * It signals a false otherwise. 
	 * @param <T> the source element type.
	 * @param source the source observable
	 * @param predicate the predicate to test the values
	 * @return the observable.
	 */
	public static <T> Observable<Boolean> any(final Observable<T> source, final Func1<Boolean, T> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<T>() {
					/** Are we done? */
					boolean done;
					@Override
					public void next(T value) {
						if (!done) {
							if (predicate.invoke(value)) {
								done = true;
								observer.next(true);
								observer.finish();
							}
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (!done) {
							observer.next(false);
							observer.finish();
						}
					}
					
				});
			}
		};
	}
	/**
	 * Computes and signals the average value of the integer source.
	 * The source may not send nulls.
	 * @param source the source of integers to aggregate.
	 * @return the observable for the average value
	 */
	public static Observable<Double> averageInt(final Observable<Integer> source) {
		return new Observable<Double>() {
			@Override
			public Closeable register(final Observer<? super Double> observer) {
				return source.register(new Observer<Integer>() {
					/** The sum of the values thus far. */
					double sum;
					/** The number of values. */
					int count;
					@Override
					public void next(Integer value) {
						sum += value.doubleValue();
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(sum / count);
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Computes and signals the average value of the Long source.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the average value
	 */
	public static Observable<Double> averageLong(final Observable<Long> source) {
		return new Observable<Double>() {
			@Override
			public Closeable register(final Observer<? super Double> observer) {
				return source.register(new Observer<Long>() {
					/** The sum of the values thus far. */
					double sum;
					/** The number of values. */
					int count;
					@Override
					public void next(Long value) {
						sum += value.doubleValue();
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(sum / count);
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Computes and signals the average value of the Double source.
	 * The source may not send nulls.
	 * @param source the source of Doubles to aggregate.
	 * @return the observable for the average value
	 */
	public static Observable<Double> averageDouble(final Observable<Double> source) {
		return new Observable<Double>() {
			@Override
			public Closeable register(final Observer<? super Double> observer) {
				return source.register(new Observer<Double>() {
					/** The sum of the values thus far. */
					double sum;
					/** The number of values. */
					int count;
					@Override
					public void next(Double value) {
						sum += value.doubleValue();
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(sum / count);
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Computes and signals the average value of the BigInteger source.
	 * The source may not send nulls.
	 * @param source the source of BigIntegers to aggregate.
	 * @return the observable for the average value
	 */
	public static Observable<BigDecimal> averageBigInteger(final Observable<BigInteger> source) {
		return new Observable<BigDecimal>() {
			@Override
			public Closeable register(final Observer<? super BigDecimal> observer) {
				return source.register(new Observer<BigInteger>() {
					/** The sum of the values thus far. */
					BigDecimal sum = BigDecimal.ZERO;
					/** The number of values. */
					int count;
					@Override
					public void next(BigInteger value) {
						sum = sum.add(new BigDecimal(value));
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(sum.divide(new BigDecimal(count), 9, BigDecimal.ROUND_HALF_UP));
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Computes and signals the average value of the BigDecimal source.
	 * The source may not send nulls.
	 * @param source the source of BigDecimals to aggregate.
	 * @return the observable for the average value
	 */
	public static Observable<BigDecimal> averageBigDecimal(final Observable<BigDecimal> source) {
		return new Observable<BigDecimal>() {
			@Override
			public Closeable register(final Observer<? super BigDecimal> observer) {
				return source.register(new Observer<BigDecimal>() {
					/** The sum of the values thus far. */
					BigDecimal sum = BigDecimal.ZERO;
					/** The number of values. */
					int count;
					@Override
					public void next(BigDecimal value) {
						sum = sum.add(value);
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(sum.divide(new BigDecimal(count), 9, BigDecimal.ROUND_HALF_UP));
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Buffer the nodes as they become available and send them out in bufferSize chunks.
	 * The observers return a new and modifiable list of T on every next() call.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param bufferSize the target buffer size
	 * @return the observable of the list
	 */
	public static <T> Observable<List<T>> buffer(final Observable<T> source, final int bufferSize) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current buffer. */
					List<T> buffer;

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
					
				});
			}
		};
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
	public static <T> Observable<List<T>> buffer(final Observable<T> source, final long time, final TimeUnit unit, final ScheduledExecutorService pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final ScheduledFuture<?> schedule = pool.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
					}
				}, time, time, unit);
				return source.register(new Observer<T>() {
					/** The buffer to fill in. */
					@Override
					public void next(T value) {
						buffer.add(value);
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						schedule.cancel(false);
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
						observer.finish();
					}
				});
			}
		};
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * The scheduling is done on the default ScheduledExecutorService.
	 * @param <T> the type of elements to observe
	 * @param source the source of Ts.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @return the observable of list of Ts
	 */
	public static <T> Observable<List<T>> buffer(final Observable<T> source, final long time, final TimeUnit unit) {
		return buffer(source, time, unit, DEFAULT_SCHEDULED_POOL);
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
	public static <T> Observable<List<T>> buffer(final Observable<T> source, 
			final int bufferSize, final long time, final TimeUnit unit, 
			final ScheduledExecutorService pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final AtomicInteger bufferLength = new AtomicInteger();
				final ScheduledFuture<?> schedule = pool.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());
						observer.next(curr);
					}
				}, time, time, unit);
				return source.register(new Observer<T>() {
					/** The buffer to fill in. */
					@Override
					public void next(T value) {
						buffer.add(value);
						if (bufferLength.incrementAndGet() == bufferSize) {
							List<T> curr = new ArrayList<T>();
							buffer.drainTo(curr);
							bufferLength.addAndGet(-curr.size());
							observer.next(curr);
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						schedule.cancel(false);
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
						observer.finish();
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
	public static <T> Observable<List<T>> buffer(final Observable<T> source, 
			final int bufferSize, final long time, final TimeUnit unit) {
		return buffer(source, bufferSize, time, unit, DEFAULT_SCHEDULED_POOL);
	}
	/**
	 * Combines the notifications of all sources. The resulting stream of Ts might come from any of the sources.
	 * @param <T> the type of the values
	 * @param sources the list of sources
	 * @return the observable
	 */
	public static <T> Observable<T> merge(final Iterable<Observable<T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final List<Closeable> disposables = new ArrayList<Closeable>();
				for (Observable<T> os : sources) {
					disposables.add(os.register(observer));
				}
				return new Closeable() {
					@Override
					public void close() {
						for (Closeable d : disposables) {
							try {
								d.close();
							} catch (IOException e) {
							}
						}
					}
				};
			}
		};
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
	public static <T> Observable<Boolean> contains(final Observable<T> source, final T value) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<T>() {
					/** Are we finished? */
					boolean done;
					@Override
					public void next(T x) {
						if (!done) {
							if (x == value || (x != null && x.equals(value))) {
								done = true;
								observer.next(true);
								observer.finish();
							}
						}
					}

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
					
				});
			}
		};
	}
	/**
	 * Counts the number of elements in the observable source.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the count signal
	 */
	public static <T> Observable<Integer> count(final Observable<T> source) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				return source.register(new Observer<T>() {
					/** The counter. */
					int count;
					@Override
					public void next(T value) {
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
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
	public static <T> Observable<Long> countLong(final Observable<T> source) {
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				return source.register(new Observer<T>() {
					/** The counter. */
					long count;
					@Override
					public void next(T value) {
						count++;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
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
	public static <T> Observable<T> defer(final Func0<Observable<T>> observableFactory) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return observableFactory.invoke().register(observer);
			}
		};
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
	public static <T> Observable<T> delay(final Observable<T> source, final long time, final TimeUnit unit, final ScheduledExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void next(final T value) {
						pool.schedule(new Runnable() {
							@Override
							public void run() {
								observer.next(value);
							}
						}, time, unit);
					}

					@Override
					public void error(final Throwable ex) {
						pool.schedule(new Runnable() {
							@Override
							public void run() {
								observer.error(ex);
							}
						}, time, unit);
					}

					@Override
					public void finish() {
						pool.schedule(new Runnable() {
							@Override
							public void run() {
								observer.finish();
							}
						}, time, unit);
					}
					
				});
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
	public static <T> Observable<T> delay(final Observable<T> source, final long time, final TimeUnit unit) {
		return delay(source, time, unit, DEFAULT_SCHEDULED_POOL);
	}
	/**
	 * Creates an observer with debugging purposes. It prints the submitted values, the exceptions.
	 * @param <T> the value type
	 * @return the observer
	 */
	public static <T> Observer<T> printlnObserver() {
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
	 * Returns an observable which fires next() events only when the subsequent values differ
	 * in terms of Object.equals().
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @return the observable
	 */
	public static <T> Observable<T> distinct(final Observable<T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The last value. */
					T last;
					/** Indication as the first. */
					boolean first = true;
					@Override
					public void next(T value) {
						if ((last == value || (last != null && last.equals(value))) || first) {
							last = value;
							first = false;
							observer.next(value);
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
		};
	}
	/**
	 * Returns Ts from the source observable if the subsequent keys extracted by <code>keyExtractor</code> are different.
	 * @param <T> the type of the values to observe
	 * @param <U> the key type check for distinction
	 * @param source the source of Ts
	 * @param keyExtractor the etractor for the keys
	 * @return the new filtered observable
	 */
	public static <T, U> Observable<T> distinct(final Observable<T> source, final Func1<U, T> keyExtractor) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The last value. */
					U lastKey;
					/** Indication as the first. */
					boolean first = true;
					@Override
					public void next(T value) {
						U key = keyExtractor.invoke(value);
						if ((lastKey == key || (lastKey != null && lastKey.equals(key))) || first) {
							lastKey = key;
							first = false;
							observer.next(value);
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
	public static <T> Observable<T> invoke(final Observable<T> source, final Action1<T> action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void next(T value) {
						action.invoke(value);
						observer.next(value);
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
	public static <T> Observable<T> invoke(final Observable<T> source, final Observer<T> observer) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> o) {
				return source.register(new Observer<T>() {
					@Override
					public void next(T value) {
						observer.next(value);
						o.next(value);
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex); // FIXME should this also happen?
						o.error(ex);
					}

					@Override
					public void finish() {
						observer.finish(); // FIXME should this also happen?
						o.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Relay the stream of Ts until condition turns into false.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param condition the condition that must hold to relay Ts
	 * @return the new observable
	 */
	public static <T> Observable<T> relayUntil(final Observable<T> source, final Func0<Boolean> condition) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Are we done? */
					boolean done;
					@Override
					public void next(T value) {
						if (!done) {
							done |= !condition.invoke();
							if (!done) {
								observer.next(value);
							}
						}
					}

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
							observer.finish();
						}
					}
					
				});
			}
		};
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
	/*public */static <T> Observable<Void> drain(final Observable<T> source, final Func1<Observable<Void>, T> pump, final ExecutorService pool) {
		return new Observable<Void>() {
			@Override
			public Closeable register(final Observer<? super Void> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void next(T value) {
						// TODO Auto-generated method stub
						Observable<Void> o2 = pump.invoke(value);
						observeOn(o2, pool).register(observer); // FIXME I don't understand
					}

					@Override
					public void error(Throwable ex) {
						// TODO Auto-generated method stub
						throw new UnsupportedOperationException();
					}

					@Override
					public void finish() {
						// TODO Auto-generated method stub
						throw new UnsupportedOperationException();
						
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
	/*public */static <T> Observable<Void> drain(final Observable<T> source, final Func1<Observable<Void>, T> pump) {
		return drain(source, pump, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Returns an empty observable which signals only finish() on the given pool.
	 * @param <T> the expected type, (irrelevant)
	 * @param pool the pool to invoke the the finish()
	 * @return the observable
	 */
	public static <T> Observable<T> empty(final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						observer.finish();
					}
				});
				return EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * @param <T> the type of the values to observe (irrelevant)
	 * @return Returns an empty observable which signals only finish() on the default observer pool.
	 */
	public static <T> Observable<T> empty() {
		return empty(DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Invokes the given action when the source signals a finish() or error().
	 * @param <T> the type of the observed values
	 * @param source the source of Ts
	 * @param action the action to invoke on finish() or error()
	 * @return the new observable
	 */
	public static <T> Observable<T> finish(final Observable<T> source, final Action0 action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void next(T value) {
						observer.next(value);
					}

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
	public static <T> T first(final Observable<T> source) {
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
	public static <T, U> Observable<U> forEach(final Iterable<T> source, final Func1<Observable<U>, T> selector) {
		List<Observable<U>> list = new ArrayList<Observable<U>>();
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
	public static <T> Observable<List<T>> forkJoin(final Iterable<Observable<T>> sources) {
		final List<AtomicReference<T>> lastValues = new ArrayList<AtomicReference<T>>();
		final List<Observable<T>> observableList = new ArrayList<Observable<T>>();
		for (Observable<T> o : sources) {
			observableList.add(o);
			lastValues.add(new AtomicReference<T>());
		}
		final AtomicInteger wip = new AtomicInteger(observableList.size());
		
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				int i = 0;
				for (Observable<T> o : observableList) {
					final int j = i;
					
					o.register(new Observer<T>() {
						/** The last value. */
						T last;
						@Override
						public void next(T value) {
							last = value;
						}

						@Override
						public void error(Throwable ex) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void finish() {
							lastValues.get(j).set(last);
							if (wip.decrementAndGet() == 0) {
								List<T> values = new ArrayList<T>();
								for (AtomicReference<T> r : lastValues) {
									values.add(r.get());
								}
								observer.next(values);
								observer.finish();
							}
						}
						
					});
					
					i++;
				}
				return EMPTY_CLOSEABLE;
			}
		};
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
	public static <T, U> Observable<U> generate(final T initial, final Func1<Boolean, T> condition, 
			final Func1<T, T> next, final Func1<U, T> selector, final ExecutorService pool) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				// the cancellation indicator
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						T t = initial;
						while (condition.invoke(t) && !cancel.get()) {
							observer.next(selector.invoke(t));
							t = next.invoke(t);
						}
						if (!cancel.get()) {
							observer.finish();
						}
					}
				});
				return new Closeable() {
					@Override
					public void close() {
						cancel.set(true);
					}
				};
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
	public static <T, U> Observable<U> generate(final T initial, final Func1<Boolean, T> condition, 
			final Func1<T, T> next, final Func1<U, T> selector) {
		return generate(initial, condition, next, selector, DEFAULT_OBSERVABLE_POOL);
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
	public static <T, U> Observable<Timestamped<U>> generateTimed(final T initial, final Func1<Boolean, T> condition, 
			final Func1<T, T> next, final Func1<U, T> selector, final Func1<Long, T> delay, final ScheduledExecutorService pool) {
		return new Observable<Timestamped<U>>() {
			@Override
			public Closeable register(final Observer<? super Timestamped<U>> observer) {
				// the cancellation indicator
				final AtomicBoolean cancel = new AtomicBoolean();
				
				if (condition.invoke(initial)) {
					pool.schedule(new Runnable() {
						T current = initial;
						@Override
						public void run() {
							observer.next(Timestamped.of(selector.invoke(current), System.currentTimeMillis()));
							final T tn = next.invoke(current);
							current = tn;
							if (condition.invoke(tn) && !cancel.get()) {
								pool.schedule(this, delay.invoke(tn), TimeUnit.MILLISECONDS);
							} else {
								if (!cancel.get()) {
									observer.finish();
								}
							}
							
						}
					}, delay.invoke(initial), TimeUnit.MILLISECONDS);
				}
				
				return new Closeable() {
					@Override
					public void close() {
						cancel.set(true);
					}
				};
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
	public static <T, U> Observable<Timestamped<U>> generateTimed(final T initial, final Func1<Boolean, T> condition, 
			final Func1<T, T> next, final Func1<U, T> selector, final Func1<Long, T> delay) {
		return generateTimed(initial, condition, next, selector, delay, DEFAULT_SCHEDULED_POOL);
	}
	/**
	 * An observer implementation which keeps track of the registered observers and
	 * common methods which dispach events to all registered observables.
	 * @author akarnokd, 2011.01.29.
	 * @param <T> the element type of the observable.
	 */
	static class RegisteringObservable<T> implements Observable<T>, Observer<T> {
		/** The map of the active observers. */
		final ConcurrentMap<Observer<? super T>, Object> observers = new ConcurrentHashMap<Observer<? super T>, Object>();
		/** The default element for the map. */
		static final Object VALUE = new Object();
		@Override
		public void next(T value) {
			for (Observer<? super T> os : observers.keySet()) {
				os.next(value);
			}
		}

		@Override
		public void error(Throwable ex) {
			for (Observer<? super T> os : observers.keySet()) {
				os.error(ex);
			}
		}

		@Override
		public void finish() {
			for (Observer<? super T> os : observers.keySet()) {
				os.finish();
			}
		}

		@Override
		public Closeable register(final Observer<? super T> observer) {
			observers.put(observer,  VALUE);
			return new Closeable() {
				@Override
				public void close() throws IOException {
					observers.remove(observer);
				}
			};
		}
	}
	/**
	 * A variant of the registering observable which stores a group key.
	 * @author akarnokd, 2011.01.29.
	 * @param <Key> the type of the key
	 * @param <Value> the value type
	 */
	static class GroupedRegisteringObservable<Key, Value> extends RegisteringObservable<Value> implements GroupedObservable<Key, Value> {
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
	public static <T, Key> Observable<GroupedObservable<Key, T>> groupBy(final Observable<T> source, final Func1<Key, T> keyExtractor) {
		return groupBy(source, keyExtractor, Functions.<T>identity());
	}
	/**
	 * Concatenate two observables in a way when the first finish() the second is registered
	 * and continued with.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the concatenated observable
	 */
	public static <T> Observable<T> concat(Observable<T> first, Observable<T> second) {
		List<Observable<T>> list = new ArrayList<Observable<T>>();
		list.add(first);
		list.add(second);
		return concat(list);
	}
	/**
	 * Use the mapper to transform the T source into an U source.
	 * @param <T> the type of the original observable
	 * @param <U> the type of the new observable
	 * @param source the source of Ts
	 * @param mapper the mapper from Ts to Us
	 * @return the observable on Us
	 */
	public static <T, U> Observable<U> transform(final Observable<T> source, final Func1<U, T> mapper) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void next(T value) {
						observer.next(mapper.invoke(value));
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
		};
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
	public static <T, U, Key> Observable<GroupedObservable<Key, U>> groupBy(final Observable<T> source, final Func1<Key, T> keyExtractor, final Func1<U, T> valueExtractor) {
		final ConcurrentMap<Key, GroupedRegisteringObservable<Key, U>> knownGroups = new ConcurrentHashMap<Key, GroupedRegisteringObservable<Key, U>>();
		return new Observable<GroupedObservable<Key, U>>() {
			@Override
			public Closeable register(
					final Observer<? super GroupedObservable<Key, U>> observer) {
				return source.register(new Observer<T>() {
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
					
				});
			}
		};
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
	public static <T> Observable<T> ifThen(final Func0<Boolean> condition, final Observable<T> then, final Observable<T> orElse) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Closeable s1 = then.register(new Observer<T>() {

					@Override
					public void next(T value) {
						if (condition.invoke()) {
							observer.next(value);
						}
					}

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
					
				});
				final Closeable s2 = orElse.register(new Observer<T>() {

					@Override
					public void next(T value) {
						if (!condition.invoke()) {
							observer.next(value);
						}
					}

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
					
				});
				
				return new Closeable() {
					@Override
					public void close() throws IOException {
						try {
							s1.close();
						} catch (IOException ex) {
							
						}
						try {
							s2.close();
						} catch (IOException ex) {
							
						}
					}
				};
			}
		};
	}
	/**
	 * Returns an observable which never fires.
	 * @param <T> the type of the observable, irrelevant
	 * @return the observable
	 */
	public static <T> Observable<T> never() {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return EMPTY_CLOSEABLE;
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
	public static <T> Observable<T> ifThen(final Func0<Boolean> condition, final Observable<T> then) {
		return ifThen(condition, then, Observables.<T>never());
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * @param start the starting value of the tick
	 * @param end the finishing value of the tick
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @param pool the scheduler pool for the wait
	 * @return the observer
	 */
	public static Observable<Long> tick(final long start, final long end, final long delay, final TimeUnit unit, final ScheduledExecutorService pool) {
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				final AtomicReference<ScheduledFuture<?>> f = new AtomicReference<ScheduledFuture<?>>();
				final AtomicBoolean cancelled = new AtomicBoolean();
				f.set(pool.schedule(new Runnable() {
					long current = start;
					@Override
					public void run() {
						if (current < end && !cancelled.get()) {
							observer.next(current++);
							f.set(pool.schedule(this, delay, unit));
						} else {
							if (!cancelled.get()) {
								observer.finish();
							}
						}
					}
				}, delay, unit));
				return new Closeable() {
					@Override
					public void close() throws IOException {
						cancelled.set(true);
						f.get().cancel(false);
					}
				};
			}
		};
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param start the starting value of the tick
	 * @param end the finishing value of the tick
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	public static Observable<Long> tick(final long start, final long end, final long delay, final TimeUnit unit) {
		return tick(start, end, delay, unit, DEFAULT_SCHEDULED_POOL);
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	public static Observable<Long> tick(final long delay, final TimeUnit unit) {
		return tick(0, Long.MAX_VALUE, delay, unit, DEFAULT_SCHEDULED_POOL);
	}
	/**
	 * Merge the events of two observable sequences.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the merged observable
	 */
	public static <T> Observable<T> merge(Observable<T> first, Observable<T> second) {
		List<Observable<T>> list = new ArrayList<Observable<T>>();
		list.add(first);
		list.add(second);
		return merge(list);
	}
	/**
	 * Signals true if the source observable fires finish() without ever firing next().
	 * This means once the next() is fired, the resulting observer will return early.
	 * @param source the source observable of any type
	 * @return the observer
	 */
	public static Observable<Boolean> isEmpty(final Observable<?> source) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<Object>() {
					/** We already determined the answer? */
					boolean done;
					@Override
					public void next(Object value) {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					}

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
	public static <T> T last(final Observable<T> source) {
		final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
		Closeable c = source.register(new Observer<T>() {
			/** The current value. */
			T current;
			/** Are we the first? */
			boolean first = true;
			@Override
			public void next(T value) {
				first = false;
				current = value;
			}

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
	public static <T> Iterable<T> latest(final Observable<T> source) {
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
					public void next(T value) {
						if (once) {
							once = false;
							hasValue.set(true);
						}
						current.set(value);
						first.countDown();
					}

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
					
				});
				return new Iterator<T>() {
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
					@Override
					protected void finalize() throws Throwable {
						c.close();
					}
				};
			}
		};
	};
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the maximum value
	 */
	public static <T extends Comparable<? super T>> Observable<T> max(final Observable<T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Keeps track of the maximum value. */
					T maxValue;
					/** Is this the first original value? */
					boolean first = true;
					@Override
					public void next(T value) {
						if (first || maxValue.compareTo(value) < 0) {
							first = false;
							maxValue = value;
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (!first) {
							observer.next(maxValue);
						}
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the maximum value
	 */
	public static <T> Observable<T> max(final Observable<T> source, final Comparator<T> comparator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Keeps track of the maximum value. */
					T maxValue;
					/** Is this the first original value? */
					boolean first = true;
					@Override
					public void next(T value) {
						if (first || comparator.compare(maxValue, value) < 0) {
							first = false;
							maxValue = value;
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (!first) {
							observer.next(maxValue);
						}
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the minimum value
	 */
	public static <T extends Comparable<? super T>> Observable<T> min(final Observable<T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Keeps track of the maximum value. */
					T minValue;
					/** Is this the first original value? */
					boolean first = true;
					@Override
					public void next(T value) {
						if (first || minValue.compareTo(value) > 0) {
							first = false;
							minValue = value;
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (!first) {
							observer.next(minValue);
						}
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the minimum value
	 */
	public static <T> Observable<T> min(final Observable<T> source, final Comparator<T> comparator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Keeps track of the maximum value. */
					T minValue;
					/** Is this the first original value? */
					boolean first = true;
					@Override
					public void next(T value) {
						if (first || comparator.compare(minValue, value) > 0) {
							first = false;
							minValue = value;
						}
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (!first) {
							observer.next(minValue);
						}
						observer.finish();
					}
					
				});
			}
		};
	}
	/** The diagnostic states of the current runnable. */
	enum ObserverState { OBSERVER_RUNNING, OBSERVER_FINISHED, OBSERVER_ERROR };
	/**
	 * Constructs an observer which logs errors in case next(), finish() or error() is called 
	 * and the observer is not in running state anymore due an earlier finish() or error() call.
	 * @param <T> the element type.
	 * @param source the source observable
	 * @return the augmented observable
	 */
	public static <T> Observable<T> debugState(final Observable<T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					ObserverState state = ObserverState.OBSERVER_RUNNING;
					@Override
					public void next(T value) {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						observer.next(value);
					}

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
					
				});
			}
		};
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
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> maxBy(final Observable<T> source, final Func1<Key, T> keyExtractor) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key maxKey;
					@Override
					public void next(T value) {
						Key key = keyExtractor.invoke(value);
						if (collect == null) {
							maxKey = key;
							collect = new ArrayList<T>();
							collect.add(value);
						} else {
							int order = maxKey.compareTo(key);
							if (order == 0) {
								collect.add(value);
							} else
							if (order < 0) {
								maxKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}

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
					
				});
			}
		};
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
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> minBy(final Observable<T> source, final Func1<Key, T> keyExtractor) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key minKey;
					@Override
					public void next(T value) {
						Key key = keyExtractor.invoke(value);
						if (collect == null) {
							minKey = key;
							collect = new ArrayList<T>();
							collect.add(value);
						} else {
							int order = minKey.compareTo(key);
							if (order == 0) {
								collect.add(value);
							} else
							if (order > 0) {
								minKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}

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
					
				});
			}
		};
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
	public static <T, Key> Observable<List<T>> maxBy(final Observable<T> source, final Func1<Key, T> keyExtractor, 
			final Comparator<Key> keyComparator) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key maxKey;
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
							if (order < 0) {
								maxKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}

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
					
				});
			}
		};
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
	 */
	public static <T, Key> Observable<List<T>> minBy(final Observable<T> source, final Func1<Key, T> keyExtractor, 
			final Comparator<Key> keyComparator) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key minKey;
					@Override
					public void next(T value) {
						Key key = keyExtractor.invoke(value);
						if (collect == null) {
							minKey = key;
							collect = new ArrayList<T>();
							collect.add(value);
						} else {
							int order = keyComparator.compare(minKey, key);
							if (order == 0) {
								collect.add(value);
							} else
							if (order > 0) {
								minKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}

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
					
				});
			}
		};
	}
	/**
	 * Filters objects from source which are assignment compatible with T.
	 * Note that due java erasure complex generic types can't be filtered this way in runtime (e.g., List&lt;String>.class is just List.class).
	 * @param <T> the type of the expected values
	 * @param source the source of unknown elements
	 * @param token the token to test agains the elements
	 * @return the observable containing Ts
	 */
	public static <T> Observable<T> typedAs(final Observable<?> source, final Class<T> token) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<Object>() {
					@Override
					public void next(Object value) {
						if (token.isInstance(value)) {
							observer.next(token.cast(value));
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
		};
	}
	/**
	 * A wrapper implementation for observer which is able to unregister from the Observable.
	 * Use the registerWith() and unregister() methods instead of adding this to a register() call.
	 * @author akarnokd, 2011.01.29.
	 * @param <T> the element type to observe
	 */
	abstract static class UObserver<T> implements Observer<T> {
		/** The saved handler. */ 
		protected Closeable handler;
		/**
		 * Register with the given observable.
		 * @param observable the target observable
		 * @return the unregistration handler
		 */
		public Closeable registerWith(Observable<T> observable) {
			handler = observable.register(this);
			return handler;
		}
		/**
		 * Unregisters this observer from its observable.
		 */
		protected void unregister() {
			try {
				handler.close();
			} catch (IOException e) {
				throw new RuntimeException();
			}
		}
	}
	/**
	 * Concatenates the source observables in a way that when the first finish(), the
	 * second gets registered and continued, and so on.
	 * FIXME not sure how it should handle closability
	 * @param <T> the type of the values to observe
	 * @param sources the source list of subsequent observables
	 * @return the concatenated observable
	 */
	public static <T> Observable<T> concat(final Iterable<Observable<T>> sources) {
		final Iterator<Observable<T>> it = sources.iterator();
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				if (it.hasNext()) {
					UObserver<T> obs = new UObserver<T>() {
						@Override
						public void next(T value) {
							observer.next(value);
						}

						@Override
						public void error(Throwable ex) {
							observer.error(ex);
						}

						@Override
						public void finish() {
							unregister();
							registerWith(it.next());
						}
						
					};
					return obs.registerWith(it.next());
				}
				observer.finish();
				return EMPTY_CLOSEABLE;
			}
		};
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
	public static <T> Observable<T> resumeAlways(final Iterable<Observable<T>> sources) {
		final Iterator<Observable<T>> it = sources.iterator();
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				if (it.hasNext()) {
					UObserver<T> obs = new UObserver<T>() {
						@Override
						public void next(T value) {
							observer.next(value);
						}

						@Override
						public void error(Throwable ex) {
							unregister();
							registerWith(it.next());
						}

						@Override
						public void finish() {
							unregister();
							registerWith(it.next());
						}
						
					};
					return obs.registerWith(it.next());
				}
				observer.finish();
				return EMPTY_CLOSEABLE;
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
	public static <T> Observable<T> catchException(final Iterable<Observable<T>> sources) {
		final Iterator<Observable<T>> it = sources.iterator();
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				if (it.hasNext()) {
					UObserver<T> obs = new UObserver<T>() {
						@Override
						public void next(T value) {
							observer.next(value);
						}

						@Override
						public void error(Throwable ex) {
							unregister();
							registerWith(it.next());
						}

						@Override
						public void finish() {
							unregister();
							observer.finish();
						}
						
					};
					return obs.registerWith(it.next());
				}
				observer.finish();
				return EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * Unwrap the values within a timestamped observable to its normal value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	public static <T> Observable<T> removeTimestamped(Observable<Timestamped<T>> source) {
		return transform(source, Functions.<T>unwrapTimestamped());
	}
	/**
	 * Wrap the values within a observable to a timestamped value having always
	 * the System.currentTimeMillis() value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	public static <T> Observable<Timestamped<T>> addTimestamped(Observable<T> source) {
		return transform(source, Functions.<T>wrapTimestamped());
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
	public static <T> Observable<T> repeat(final T value, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						while (!cancel.get()) {
							observer.next(value);
						}
					}
				});
				return new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
					}
				};
			}
		};
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
	public static <T> Observable<T> repeat(final T value, final int count, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						int i = count;
						while (!cancel.get() && i-- > 0) {
							observer.next(value);
						}
					}
				});
				return new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
					}
				};
			}
		};
	}
	/**
	 * Creates an observable which repeates the given value indefinitely
	 * and runs on the default pool. Note that the observers must
	 * deregister to stop the infinite background loop
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(final T value) {
		return repeat(value, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Creates an observable which repeates the given value <code>count</code> times
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(final T value, final int count) {
		return repeat(value, count, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @param pool the pool where the generator loop runs
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(final Func0<T> func, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						while (!cancel.get()) {
							observer.next(func.invoke());
						}
					}
				});
				return new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
					}
				};
			}
		};
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
	public static <T> Observable<T> repeat(final Func0<T> func, final int count, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicBoolean cancel = new AtomicBoolean();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						int i = count;
						while (!cancel.get() && i-- > 0) {
							observer.next(func.invoke());
						}
						observer.finish();
					}
				});
				return new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
					}
				};
			}
		};
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the default pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(final Func0<T> func) {
		return repeat(func, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Creates an observable which repeatedly calls the given function <code>count</code> times to generate Ts
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param func the function to call to generate values
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(final Func0<T> func, final int count) {
		return repeat(func, count, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Restarts the observation until the source observable terminates normally.
	 * @param <T> the type of elements
	 * @param source the source observable
	 * @return the repeating observable
	 */
	public static <T> Observable<T> retry(final Observable<T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				UObserver<T> obs = new UObserver<T>() {
					@Override
					public void next(T value) {
						observer.next(value);
					}

					@Override
					public void error(Throwable ex) {
						unregister();
						registerWith(source);
					}

					@Override
					public void finish() {
						unregister();
						observer.finish();
					}
					
				};
				return obs.registerWith(source);
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
	public static <T> Observable<T> retry(final Observable<T> source, final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				UObserver<T> obs = new UObserver<T>() {
					/** The remaining retry count. */
					int remainingCount = count;
					@Override
					public void next(T value) {
						observer.next(value);
					}

					@Override
					public void error(Throwable ex) {
						unregister();
						if (remainingCount-- > 0) {
							registerWith(source);
						} else {
							observer.error(ex); // FIXME not sure
						}
					}

					@Override
					public void finish() {
						unregister();
						observer.finish();
					}
					
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Returns the single value in the observables.
	 * @param <T> the value type
	 * @param value the value
	 * @param pool the pool where to submit the value to the observers
	 * @return the observable
	 */
	public static <T> Observable<T> singleton(final T value, final ExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						observer.next(value);
						observer.finish();
					}
				});
				return EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * Returns the single value in the observables.
	 * @param <T> the value type
	 * @param value the value
	 * @return the observable
	 */
	public static <T> Observable<T> singleton(final T value) {
		return singleton(value, DEFAULT_OBSERVABLE_POOL);
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are ignored.
	 * @param source the source observable
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static void run(final Observable<?> source) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new Observer<Object>() {
			/** Are we finished? */
			boolean done;
			@Override
			public void next(Object value) {
				
			}

			@Override
			public void error(Throwable ex) {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}

			@Override
			public void finish() {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}
			
		});
		try {
			latch.await();
		} finally {
			try { c.close(); } catch (IOException ex) { }
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
	static boolean run(final Observable<?> source, long time, TimeUnit unit) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new Observer<Object>() {
			/** Are we finished? */
			boolean done;
			@Override
			public void next(Object value) {
				
			}

			@Override
			public void error(Throwable ex) {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}

			@Override
			public void finish() {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}
			
		});
		try {
			return latch.await(time, unit);
		} finally {
			try { c.close(); } catch (IOException ex) { }
		}
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are submitted to the given action.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param action the action to invoke for each value
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static <T> void run(final Observable<T> source, final Action1<? super T> action) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new Observer<T>() {
			/** Are we finished? */
			boolean done;
			@Override
			public void next(T value) {
				action.invoke(value);
			}

			@Override
			public void error(Throwable ex) {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}

			@Override
			public void finish() {
				if (!done) {
					done = false;
					latch.countDown();
				}
			}
			
		});
		try {
			latch.await();
		} finally {
			try { c.close(); } catch (IOException ex) { }
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
	public static <T> void run(final Observable<T> source, final Observer<? super T> observer) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new Observer<T>() {
			/** Are we finished? */
			boolean done;
			@Override
			public void next(T value) {
				observer.next(value);
			}

			@Override
			public void error(Throwable ex) {
				if (!done) {
					done = false;
					observer.error(ex);
					latch.countDown();
				}
			}

			@Override
			public void finish() {
				if (!done) {
					done = false;
					observer.finish();
					latch.countDown();
				}
			}
			
		});
		try {
			latch.await();
		} finally {
			try { c.close(); } catch (IOException ex) { }
		}
	}
	/**
	 * The runnable instance which is aware of its scheduler's registration.
	 * @author akarnokd, 2011.01.29.
	 */
	abstract static class USchedulable implements Runnable {
		/** The holder for the registration. */
		protected ScheduledFuture<?> future;
		/**
		 * Schedule this on the given pool.
		 * @param pool the target pool
		 * @param delay the delay
		 * @param unit the unit of delay
		 * @return the future result of the registration
		 */
		public ScheduledFuture<?> scheduleOn(ScheduledExecutorService pool, long delay, TimeUnit unit) {
			future = pool.schedule(this, delay, unit);
			return future;
		}
		/**
		 * Schedule this on the given pool at a fixed rate.
		 * @param pool the target pool
		 * @param initial the initial delay
		 * @param delay the delay
		 * @param unit the unit of delay
		 * @return the future result of the registration
		 */
		public ScheduledFuture<?> scheduleOnAtFixedRate(ScheduledExecutorService pool, long initial, long delay, TimeUnit unit) {
			future = pool.scheduleAtFixedRate(this, initial, delay, unit);
			return future;
		}
		/**
		 * Schedule this on the given pool with a fixed delay.
		 * @param pool the target pool
		 * @param initial the initial delay
		 * @param delay the delay
		 * @param unit the unit of delay
		 * @return the future result of the registration
		 */
		public ScheduledFuture<?> scheduleOnWitFixedDelay(ScheduledExecutorService pool, long initial, long delay, TimeUnit unit) {
			future = pool.scheduleWithFixedDelay(this, initial, delay, unit);
			return future;
		}
		/**
		 * Cancel the current schedule.
		 * @param interruptIfRunning interrupt if currently running?
		 */
		public void cancel(boolean interruptIfRunning) {
			future.cancel(interruptIfRunning);
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
	 * @param pool the scheduler pool where the periodic submission should happen.
	 * @return the sampled observable
	 */
	public static <T> Observable<T> sample(final Observable<T> source, final long time, final TimeUnit unit, 
			final ScheduledExecutorService pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicBoolean first = new AtomicBoolean(true);
				final AtomicBoolean cancel = new AtomicBoolean();
				final AtomicReference<T> current = new AtomicReference<T>();

				final USchedulable schedule = new USchedulable() {
					@Override
					public void run() {
						if (!cancel.get()) {
							if (!first.get()) {
								observer.next(current.get());
							}
							scheduleOn(pool, time, unit);
						}
					}
				};
				final UObserver<T> obs = new UObserver<T>() {
					boolean firstNext = true;
					@Override
					public void next(T value) {
						if (firstNext) {
							firstNext = false;
							first.set(false);
						}
						current.set(value);
					}

					@Override
					public void error(Throwable ex) {
						unregister();
						cancel.set(true);
						schedule.cancel(false);
						observer.error(ex);
					}

					@Override
					public void finish() {
						unregister();
						cancel.set(true);
						schedule.cancel(false);
						observer.finish();
					}
				};
				schedule.scheduleOn(pool, time, unit);
				final Closeable c = obs.registerWith(source);
				return new Closeable() {
					@Override
					public void close() throws IOException {
						schedule.cancel(false);
						cancel.set(true);
						c.close();
					}
				};
			}
		};
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
	public static <T> Observable<T> sample(final Observable<T> source, final long time, final TimeUnit unit) {
		return sample(source, time, unit, DEFAULT_SCHEDULED_POOL);
	}
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
	public static <T> Observable<T> accumulate(final Observable<T> source, final Func2<T, T, T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Are we waiting for the first value? */
					boolean first = true;
					/** The current accumulated value. */
					T current;
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
	public static <T> Observable<T> accumulate(final Observable<T> source, final T seed, final Func2<T, T, T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current = seed;
					@Override
					public void next(T value) {
						current = accumulator.invoke(current, value);
						observer.next(current);
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
	public static <T> Observable<T> accumulate0(final Observable<T> source, final T seed, final Func2<T, T, T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current;
					/** Are we waiting for the first value? */
					boolean first = true;
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
	public static <T, U> Observable<U> transform(final Observable<T> source, final Func2<U, Integer, T> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The running index. */
					int index;
					@Override
					public void next(T value) {
						observer.next(selector.invoke(index++, value));
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
		};
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
	public static <T, U> Observable<U> transformIterable(final Observable<T> source, 
			final Func1<Iterable<U>, T> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {

					@Override
					public void next(T value) {
						for (U u : selector.invoke(value)) {
							observer.next(u);
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
	public static <T, U> Observable<U> transformObservable(final Observable<T> source, 
			final Func1<Observable<U>, T> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {

					@Override
					public void next(T value) {
						UObserver<U> obs = new UObserver<U>() {
							@Override
							public void error(Throwable ex) {
								unregister();
							}
							@Override
							public void finish() {
								unregister();
								
							}
							@Override
							public void next(U value) {
								observer.next(value);
							};
						};
						obs.registerWith(selector.invoke(value));
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
	public static <T, U> Observable<U> transformObservable(Observable<T> source, Observable<U> provider) {
		return transformObservable(source, Functions.<Observable<U>, T>constant(provider));
	}
	/**
	 * Creates an observable in which for each of Ts an observable of Vs are
	 * requested which in turn will be transformed by the resultSelector for each
	 * pair of T and V giving an U.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param <V> the intermediate element type
	 * @param source the source of Ts
	 * @param collectionSelector the selector which returns an observable of intermediate Vs
	 * @param resultSelector the selector which gives an U for a T and V
	 * @return the observable of Us
	 */
	public static <T, U, V> Observable<U> transformObservable(final Observable<T> source, 
			final Func1<Observable<V>, T> collectionSelector, final Func2<U, T, V> resultSelector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {

					@Override
					public void next(final T value) {
						UObserver<V> obs = new UObserver<V>() {

							@Override
							public void next(V x) {
								observer.next(resultSelector.invoke(value, x));
							}

							@Override
							public void error(Throwable ex) {
								unregister();
							}

							@Override
							public void finish() {
								unregister();
							}
							
						};
						obs.registerWith(collectionSelector.invoke(value));
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
		};
	}
}
