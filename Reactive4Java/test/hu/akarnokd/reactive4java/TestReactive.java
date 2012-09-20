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

import static hu.akarnokd.reactive4java.base.Functions.equal;
import static hu.akarnokd.reactive4java.base.Functions.pairUp;
import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.all;
import static hu.akarnokd.reactive4java.reactive.Reactive.any;
import static hu.akarnokd.reactive4java.reactive.Reactive.combine;
import static hu.akarnokd.reactive4java.reactive.Reactive.concat;
import static hu.akarnokd.reactive4java.reactive.Reactive.count;
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.sequenceEqual;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static hu.akarnokd.reactive4java.reactive.Reactive.skip;
import static hu.akarnokd.reactive4java.reactive.Reactive.skipLast;
import static hu.akarnokd.reactive4java.reactive.Reactive.skipWhile;
import static hu.akarnokd.reactive4java.reactive.Reactive.take;
import static hu.akarnokd.reactive4java.reactive.Reactive.takeLast;
import static hu.akarnokd.reactive4java.reactive.Reactive.takeWhile;
import static hu.akarnokd.reactive4java.reactive.Reactive.toIterable;
import static hu.akarnokd.reactive4java.reactive.Reactive.zip;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Observable;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Test the reactive operators.
 * @author Denes Harmath, 2012.07.16.
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
	 * Assert if the sequence contains only the given item.
	 * @param <T> the element type
	 * @param expected the expected value
	 * @param actual the actual sequence
	 */
	public static <T> void assertSingle(T expected, Observable<? extends T> actual) {
		String message = "expected: " + expected + "; actual: " + makeString(actual);
		assertEquals(message, expected, single(actual));
	}
	/**
	 * Tests take().
	 */
	@Test
	public void takeOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(prefix));
		assertEqual(prefix, take(o, count));
	}
	/**
	 * Tests skip().
	 */
	@Test
	public void skipOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(prefix));
		assertEqual(postfix, skip(o, count));
	}
	/**
	 * Tests takeLast().
	 */
	@Test
	public void takeLastOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(postfix));
		assertEqual(postfix, takeLast(o, count));
	}
	/**
	 * Tests skipLast().
	 */
	@Test
	public void skipLastOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(postfix));
		assertEqual(prefix, skipLast(o, count));
	}
	/**
	 * Tests takeWhile() with some elements taken.
	 */
	@Test
	public void takeWhileSome() {
		Integer value = 42;
		Observable<Integer> prefix = from(value, value);
		Observable<Integer> postfix = from(0, value);
		Observable<Integer> o = concat(prefix, postfix);
		assertEqual(prefix, takeWhile(o, equal(value)));
	}
	/**
	 * Tests takeWhile() with all elements taken.
	 */
	@Test
	public void takeWhileAll() {
		Integer value = 42;
		Observable<Integer> o = from(value, value);
		assertEqual(o, takeWhile(o, equal(value)));
	}
	/**
	 * Tests takeWhile() with no elements taken.
	 */
	@Test
	public void takeWhileNone() {
		Integer value = 42;
		assertEqual(empty(), takeWhile(from(0, value), equal(value)));
	}
	/**
	 * Tests skipWhile() with some elements skipped.
	 */
	@Test
	public void skipWhileSome() {
		Integer value = 42;
		Observable<Integer> prefix = from(value, value);
		Observable<Integer> postfix = from(0, value);
		Observable<Integer> o = concat(prefix, postfix);
		assertEqual(postfix, skipWhile(o, equal(value)));
	}
	/**
	 * Tests skipWhile() with all elements skipped.
	 */
	@Test
	public void skipWhileAll() {
		Integer value = 42;
		assertEqual(empty(), skipWhile(from(value, value), equal(value)));
	}
	/**
	 * Tests skipWhile() with no elements skipped.
	 */
	@Test
	public void skipWhileNone() {
		Integer value = 42;
		ObservableBuilder<Integer> o = from(0, value);
		assertEqual(o, skipWhile(o, equal(value)));
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
	 * Tests sequenceEqual() in case of an empty and non-empty sequence.
	 */
	@Test
	public void sequenceEqualNotBecauseEmpty() {
		assertNotEqual(from(1, 2), empty());
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
	/**
	 * Tests all() properly returning <code>true</code>.
	 */
	@Test
	public void allTrue() {
		int value = 42;
		assertSingle(true, all(from(value, value, value), equal(value)));
	}
	/**
	 * Tests all() properly returning <code>false</code>.
	 */
	@Test
	public void allFalse() {
		int value = 42;
		assertSingle(false, all(from(value, 0, value), equal(value)));
	}
	/**
	 * Tests any() properly returning <code>true</code>.
	 */
	@Test
	public void anyTrue() {
		int value = 42;
		assertSingle(true, any(from(0, value, 0), equal(value)));
	}
	/**
	 * Tests any() properly returning <code>false</code>.
	 */
	@Test
	public void anyFalse() {
		assertSingle(false, any(from(0, 0, 0), equal(1)));
	}
	/**
	 * Tests count().
	 */
	@Test
	public void countOk() {
		Collection<Integer> i = nCopies(3, 0);
		assertSingle(i.size(), count(from(i)));
	}
	/**
	 * Tests zip().
	 */
	@Test
	public void zipOk() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1);
		ObservableBuilder<Pair<Integer, Integer>> expected = from(Pair.of(a0, b0), Pair.of(a1, b1));
		assertEqual(expected, zip(a, b, pairUp()));
	}
	/**
	 * Tests combine() with value.
	 */
	@Test
	public void combineValue() {
		final int a0 = 0;
		final int a1 = 1;
		final int value = 3;
		ObservableBuilder<Integer> a = from(a0, a1);
		ObservableBuilder<List<Integer>> expected = from(asList(a0, value), asList(a1, value));
		assertEqual(expected, combine(a, value));
	}
	/**
	 * Tests combine() with observables.
	 */
	@Test
	public void combineObservables() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1);
		ObservableBuilder<List<Integer>> expected = from(asList(a0, b0), asList(a1, b1));
		assertEqual(expected, combine(asList(a, b)));
	}
}
