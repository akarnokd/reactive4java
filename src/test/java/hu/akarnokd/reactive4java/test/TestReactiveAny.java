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

import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.any;
import static hu.akarnokd.reactive4java.util.Functions.equal;

import org.junit.Test;

/**
 * Test the Reactive.any operators.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveAny {
	/**
	 * Tests any() properly returning <code>false</code>.
	 */
	@Test
	public void anyFalse() {
		TestUtil.assertSingle(false, any(from(0, 0, 0), equal(1)));
	}

	/**
	 * Tests any() properly returning <code>true</code>.
	 */
	@Test
	public void anyTrue() {
		int value = 42;
		TestUtil.assertSingle(true, any(from(0, value, 0), equal(value)));
	}

}
