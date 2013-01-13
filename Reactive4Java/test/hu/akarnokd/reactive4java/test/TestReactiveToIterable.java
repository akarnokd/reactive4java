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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Closeables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test the Reactive.toIterable operator.
 * @author akarnokd, 2013.01.12.
 */
public class TestReactiveToIterable {
	/** Run a simple value stream test. */
	@Test/*(timeout = 1000)*/
	public void test() {
		Observable<Integer> source = Reactive.range(0, 10);
		
		Iterable<Integer> iter = Reactive.toIterable(source);
		
		List<Integer> result = new ArrayList<Integer>();
		Iterator<Integer> it = iter.iterator();
		try {
			while (it.hasNext()) {
				result.add(it.next());
			}
		} finally {
			Closeables.closeSilently(it);
		}
		
		Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), result);
	}

}
