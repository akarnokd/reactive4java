/*
 * Copyright 2011-2012 David Karnok
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

import static hu.akarnokd.reactive4java.interactive.Interactive.elementsEqual;
import static hu.akarnokd.reactive4java.interactive.Interactive.join;
import static hu.akarnokd.reactive4java.reactive.Reactive.sequenceEqual;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static hu.akarnokd.reactive4java.reactive.Reactive.toIterable;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import hu.akarnokd.reactive4java.base.Closeables;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Test utility methods.
 * @author akarnokd, 2013.01.08.
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
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Iterable<? extends T> expected, Iterable<? extends T> actual, boolean eq) {
		String message = "expected: " + makeString(expected) + "; actual: " + makeString(actual);
		boolean condition = elementsEqual(expected, actual);
		assertTrue(message, eq ? condition : !condition);
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
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Observable<? extends T> expected, Observable<? extends T> actual, boolean eq) {
		String message = "expected: " + makeString(expected) + "; actual: " + makeString(actual);
		boolean condition = single(sequenceEqual(expected, actual));
		assertTrue(message, eq ? condition : !condition);
	}
	/**
	 * Compare two sequences and assert their equivalence.
	 * @param <T> the element type
	 * @param expected the expected sequence
	 * @param actual the actual sequence
	 * @param eq should they equal?
	 */
	public static <T> void assertCompare(Iterable<? extends T> expected, Observable<? extends T> actual, boolean eq) {
		String message = "expected: " + makeString(expected) + "; actual: " + makeString(actual);
		boolean condition = single(sequenceEqual(expected, actual));
		assertTrue(message, eq ? condition : !condition);
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
		String message = "expected: " + expected + "; actual: " + makeString(actual);
		assertEquals(message, expected, single(actual));
	}
}
