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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Actions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

/**
 * Utility class that helps in creating various observer instances.
 * @author akarnokd, 2013.01.10.
 * @since 0.97
 */
public final class Observers {
	/** Utility class. */
	private Observers() { }
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
			}
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
			}
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
			}
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
			}
		};
	}
	/**
	 * Creates an observer which calls the supplied callback
	 * in case of an error() or finish() event.
	 * @param <T> the element type
	 * @param callback the callback to invoke on completion
	 * @return the observer
	 */
	@Nonnull
	public static <T> Observer<T> newAsyncAwaiter(@Nonnull final Runnable callback) {
		return new Observer<T>() {
			@Override
			public void next(T value) {
				// values ignored
			}

			@Override
			public void error(Throwable ex) {
				callback.run();
			}

			@Override
			public void finish() {
				callback.run();
			}
			
		};
	}
	/**
	 * Creates an observer which calls the supplied callback
	 * in case of an error() or finish() event.
	 * @param <T> the element type
	 * @param callback the callback to invoke on completion
	 * @return the observer
	 */
	@Nonnull
	public static <T> Observer<T> newAsyncAwaiter0(@Nonnull final Action0 callback) {
		return newAsyncAwaiter(Actions.asRunnable(callback));
	}
	/**
	 * Creates an observer which counts down the latch by one
	 * in case of an error() or finish() event.
	 * @param <T> the element type
	 * @param latch the latch to count down by one
	 * @return the observer
	 */
	@Nonnull
	public static <T> Observer<T> newAsyncAwaiter(@Nonnull final CountDownLatch latch) {
		return newAsyncAwaiter(new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
	}
	/**
	 * Creates an observer which signals the condition
	 * in case of an error() or finish() event.
	 * @param <T> the element type
	 * @param lock the lock owning the condition
	 * @param cond the condition to signal
	 * @return the observer
	 */
	@Nonnull
	public static <T> Observer<T> newAsyncAwaiter(@Nonnull final Lock lock, @Nonnull final Condition cond) {
		return newAsyncAwaiter(new Runnable() {
			@Override
			public void run() {
				lock.lock();
				try {
					cond.signalAll();
				} finally {
					lock.unlock();
				}
			}
		});
	}
	/**
	 * Creates an observer which calls notifyAll on the supplied synchronization object
	 * in case of an error() or finish() event.
	 * @param <T> the element type
	 * @param syncObject the synchronization object
	 * @return the observer
	 */
	@Nonnull
	public static <T> Observer<T> newAsyncAwaiter(@Nonnull final Object syncObject) {
		return newAsyncAwaiter(new Runnable() {
			@Override
			public void run() {
				synchronized (syncObject) {
					syncObject.notifyAll();
				}
			}
		});
	}
}
