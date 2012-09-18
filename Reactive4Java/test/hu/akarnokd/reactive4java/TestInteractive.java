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

import static hu.akarnokd.reactive4java.interactive.Interactive.concat;
import static hu.akarnokd.reactive4java.interactive.Interactive.elementsEqual;
import static hu.akarnokd.reactive4java.interactive.Interactive.join;
import static hu.akarnokd.reactive4java.interactive.Interactive.size;
import static hu.akarnokd.reactive4java.interactive.Interactive.take;
import static hu.akarnokd.reactive4java.interactive.Interactive.toIterable;
import static junit.framework.Assert.assertTrue;
import java.util.Iterator;
import org.junit.Test;

/**
 * Test the interactive operators.
 * @author Harmath Denes, 2012.07.13.
 */
public class TestInteractive {
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
	 * Test take().
	 */
	@Test
	public void takeOk() {
		Iterable<Integer> prefix = toIterable(1, 2);
		Iterable<Integer> i = concat(prefix, toIterable(3, 4));
		assertEqual(take(i, size(prefix)), prefix);
	}

}
