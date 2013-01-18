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

import hu.akarnokd.reactive4java.util.ComparingHashMap;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test the ComparingHashMap behavior.
 * @author akarnokd, 2013.01.15.
 */
public class TestComparingHashMap {
	/** Simple test. */
	@Test
	public void test() {
		ComparingHashMap<Integer, Integer> map = new ComparingHashMap<Integer, Integer>();
		map.put(0, 0);
		map.put(1, 1);
		map.put(2, 2);
		
		TestUtil.assertEqual(Arrays.asList(0, 1, 2), map.values());
	}

}
