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
package hu.akarnokd.reactive4java.test;

import static hu.akarnokd.reactive4java.interactive.Interactive.join;
import static hu.akarnokd.reactive4java.reactive.Reactive.toIterable;
import hu.akarnokd.reactive4java.base.Closeables;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.query.IterableBuilder;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.DefaultScheduler;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Assert;

/**
 * Test utility methods.
 * @author akarnokd, 2013.01.08.
 * @since 0.97
 */
public final class TestUtil {
	/** Utility class. */
	private TestUtil() { }
	/**
	 * Registers to the given observable waits for it to send its elements.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the list of events
	 * @throws Exception if the source sent an exception
	 */
	public static <T> List<T> waitForAll(Observable<? extends T> source) throws Exception {
		final List<T> result = new ArrayList<T>();
		
		final LinkedBlockingDeque<Object> complete = new LinkedBlockingDeque<Object>();
		
		Closeable c = source.register(new Observer<T>() {

			@Override
			public void next(T value) {
				result.add(value);
			}

			@Override
			public void error(Throwable ex) {
				complete.add(ex);
			}

			@Override
			public void finish() {
				complete.add(new Object());
			}
			
		});
		
		Object o = complete.take();
		
		Closeables.closeSilently(c);
		
		if (o instanceof Exception) {
			throw (Exception)o;
		} else
		if (o instanceof RuntimeException) {
			throw (RuntimeException)o;
		}
		
		return result;
	}
	/**
	 * Returns a user-friendly textual representation of the given sequence.
	 * @param source the source sequence
	 * @return the output text
	 */
	public static String makeString(Iterable<?> source) {
		Iterator<String> iterator = join(source, ", ").iterator();
		return iterator.hasNext() ? iterator.next() : "";
	}
	/**
	 * Assert the equivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertEqual(Iterable<? extends T> expected, Iterable<? extends T> actual) {
		assertCompare(expected, actual, true);
	}
	/**
	 * Assert the inequivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertNotEqual(Iterable<? extends T> expected, Iterable<? extends T> actual) {
		assertCompare(expected, actual, false);
	}
	/**
	 * Creates a new ArrayList from the elements.
	 * @param <T> the element type
	 * @param elements the elements
	 * @return the list
	 */
	public static <T> List<T> newList(T... elements) {
		List<T> result = new ArrayList<T>();
		for (T t : elements) {
			result.add(t);
		}
		return result;
	}
	/**
	 * Returns a user-friendly textual representation of the given sequence.
	 * @param source the source sequence
	 * @return the output text
	 */
	public static String makeString(Observable<?> source) {
		return makeString(toIterable(source));
	}
	/**
	 * Assert the equivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertEqual(Iterable<? extends T> expected, Observable<? extends T> actual) {
		assertCompare(expected, actual, true);
	}
	/**
	 * Assert the equivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertEqual(Observable<? extends T> expected, Observable<? extends T> actual) {
		assertCompare(expected, actual, true);
	}
	/**
	 * Assert the inequivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertNotEqual(Observable<? extends T> expected, Observable<? extends T> actual) {
		assertCompare(expected, actual, false);
	}
	/**
	 * Assert the inequivalence of two sequences.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static <T> void assertNotEqual(Iterable<? extends T> expected, Observable<? extends T> actual) {
		assertCompare(expected, actual, false);
	}
	/**
	 * Assert if the sequence contains only the given item.
	 * @param <T> the element type
	 * @param expected the expected value
	 * @param actual the actual sequence
	 */
	public static <T> void assertSingle(T expected, Observable<? extends T> actual) {
		assertCompare(Collections.singleton(expected), actual, true);
	}
	/**
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Iterable<? extends T> expected, Iterable<? extends T> actual, boolean eq) {
		List<? extends T> expectedList = IterableBuilder.from(expected).toList();
		List<? extends T> actualList = IterableBuilder.from(actual).toList();
		if (eq != expectedList.equals(actualList)) {
			fail(expectedList, actualList);
		}
	}
	/**
	 * Calls the Assert.fail with a message that displays the expected and actual values as strings.
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 */
	public static void fail(Iterable<?> expected, Iterable<?> actual) {
		Assert.fail("Sequences mismatch: expected = " + makeString(expected) + ", actual = " + makeString(actual));
	}
	/**
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Observable<? extends T> expected, Observable<? extends T> actual, boolean eq) {
		DefaultScheduler scheduler = new DefaultScheduler(2);
		assertCompare(expected, actual, eq, scheduler);
		scheduler.shutdown();
	}
	/**
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 * @param scheduler the scheduler that waits for the individual sequences
	 */
	public static <T> void assertCompare(
			Observable<? extends T> expected, 
			Observable<? extends T> actual, boolean eq,
			Scheduler scheduler) {
		try {
			List<List<T>> both = Reactive.invokeAll(expected, actual, scheduler);
			
			if (eq != both.get(0).equals(both.get(1))) {
				fail(both.get(0), both.get(1));
			}
			
		} catch (InterruptedException ex) {
			Assert.fail(ex.toString());
		}
	}
	/**
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Iterable<? extends T> expected, Observable<? extends T> actual, boolean eq) {
		List<?> actualList = ObservableBuilder.from(actual).into(new ArrayList<Object>());
		if (eq != Interactive.elementsEqual(expected, actualList)) {
			fail(expected, actualList);
		}
	}
}
