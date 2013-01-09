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

import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Test the Reactive.concat methods.
 * @author akarnokd, 2013.01.08.
 * @since 0.97
 */
public class TestReactiveConcat {
	/**
	 * Tests a simple concatenation with two static singleton values.
	 * @throws Exception on error
	 */
	@Test
	public void testSimple() throws Exception {
		Observable<Integer> value = Reactive.singleton(1);
		Observable<Integer> cat = Reactive.concat(value, value);
		
		final List<Integer> result = TestUtil.waitForAll(cat);

		TestUtil.assertEqual(Arrays.asList(1, 1), result);
	}

}
