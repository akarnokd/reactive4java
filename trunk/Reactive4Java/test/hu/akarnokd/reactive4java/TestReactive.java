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
package hu.akarnokd.reactive4java;

import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.concat;
import static hu.akarnokd.reactive4java.reactive.Reactive.count;
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.sequenceEqual;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static hu.akarnokd.reactive4java.reactive.Reactive.take;
import static hu.akarnokd.reactive4java.reactive.Reactive.toIterable;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.reactive.Observable;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Test the reactive operators.
 * @author Harmath Denes, 2012.07.16.
 */
public class TestReactive {
	/**
	 * Returns a user-friendly textual representation of the given sequence.
	 * @param source the source sequence
	 * @return the output text
	 */
	public static String makeString(Observable<?> source) {
		return TestInteractive.makeString(toIterable(source));
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
	 * Tests take().
	 */
	@Test
	public void takeOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> o = concat(prefix, from(3, 4));
		Integer count = single(count(prefix));
		assertEqual(take(o, count), prefix);
	}
	/**
	 * Tests sequenceEqual() in case of equal sequences.
	 */
	@Test
	public void sequenceEqualOk() {
		Observable<Integer> o = from(1, 2);
		assertEqual(o, o);
	}
	/**
	 * Tests sequenceEqual() in case of different sequences.
	 */
	@Test
	public void sequenceEqualNotBecauseJustPrefix() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> o = concat(prefix, from(3, 4));
		assertNotEqual(o, prefix);
	}
	/**
	 * Tests the commutativity of sequenceEqual().
	 */
	@Test
	public void sequenceEqualCommutative() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> o = concat(prefix, from(3, 4));
		assertEqual(sequenceEqual(prefix, o), sequenceEqual(o, prefix));
	}
	/**
	 * Tests single() in case of 1 element.
	 */
	@Test
	public void singleOk() {
		Integer expected = 42;
		Observable<Integer> o = from(expected);
		assertEquals(expected, single(o));
	}
	/**
	 * Tests single() in case of 0 element.
	 */
	@Test(expected = NoSuchElementException.class)
	public void singleNoSuchElement() {
		single(empty());
	}
	/**
	 * Tests single() in case of more than 1 elements.
	 */
	@Test(expected = TooManyElementsException.class)
	public void singleTooManyElements() {
		single(from(1, 2));
	}

}
