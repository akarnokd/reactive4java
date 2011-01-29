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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
				pool.execute(new Runnable() {
					@Override
					public void run() {
						for (int i = start; i < start + count; i++) {
							observer.next(i);
						}
						observer.finish();
					}
				});
				return EMPTY_CLOSEABLE; // FIXME what should be disposed???
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
				
				observable.register(new Observer<T>() {
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
						return peek != Option.none();
					}
					@Override
					public T next() {
						if (peekBeforeNext) {
							peekBeforeNext = false;
							if (peek != Option.none()) {
								return peek.value();
							}
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
	 * It tries to submit the values of first observable, but when it throws an exeption,
	 * the next observable within source is used further on. Basically a failover between the Observables.
	 * If the current source finish() then the result observable calls finish().
	 * If the last of the sources calls error() the result observable calls error()
	 * @param <T> the type of the values
	 * @param sources the available source observables.
	 * @return the failover observable
	 */
	public static <T> Observable<T> catchException(final Iterable<Observable<T>> sources) {
		final Iterator<Observable<T>> it = sources.iterator();
		return new Observable<T>() {
			/** The last one to dispose. */
//			Disposable lastDisposable;
			@Override
			public Closeable register(final Observer<? super T> observer) {
				if (it.hasNext()) {
					Closeable d = it.next().register(new Observer<T>() {
						boolean done;
						@Override
						public void next(T value) {
							if (!done) {
								observer.next(value);
							}
						}

						@Override
						public void error(Throwable ex) {
							if (!done) {
								done = true;
//								Disposable d = lastDisposable;
//								if (d != null) {
//									d.close();
//								}
								register(this);
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
//					lastDisposable = d;
					return d;
				}
				return new Closeable() {
					@Override
					public void close() {
//						Disposable d = lastDisposable;
//						if (d != null) {
//							d.close();
//						}
					}
				};
			}
		};
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
	 * Concatenates the source observables in a way that when the first finish(), the
	 * second gets registered and continued, and so on.
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
					return it.next().register(new Observer<T>() {

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
							register(observer);
						}
					});
				}
				return EMPTY_CLOSEABLE;
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
}
