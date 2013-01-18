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

import junit.framework.Assert;
import hu.akarnokd.reactive4java.util.ComparingHashSet;
import hu.akarnokd.reactive4java.util.Functions;

import org.junit.Test;

/**
 * Test the comparing hashset class.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public class TestComparingHashSet {
	/** Test simple add. */
	@Test
	public void test() {
		ComparingHashSet<Integer> set = new ComparingHashSet<Integer>(Functions.equals());
		
		Assert.assertTrue(set.add(0));

		Assert.assertFalse(set.add(0));

		for (int i = 1; i < 17; i++) {
			set.add(i);
		}

		Assert.assertFalse(set.add(16));
	}

}
