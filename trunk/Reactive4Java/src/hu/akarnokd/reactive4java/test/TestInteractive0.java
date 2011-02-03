/*
 * Copyright 2011 David Karnok
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

import static hu.akarnokd.reactive4java.Functions.*;
import static hu.akarnokd.reactive4java.Interactives.*;

import hu.akarnokd.reactive4java.Func1;
import hu.akarnokd.reactive4java.Func2;
import hu.akarnokd.reactive4java.Functions;
import hu.akarnokd.reactive4java.GroupedIterable;
import hu.akarnokd.reactive4java.Interactives;
import hu.akarnokd.reactive4java.Option;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;


/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class TestInteractive0 {

	/**
	 * Utility class.
	 */
	private TestInteractive0() {
		// utility class
	}
	/** 
	 * Run the Iterable with a print attached. 
	 * @param source the iterable source
	 * waiting on the observable completion
	 */
	static void run(Iterable<?> source) {
		Interactives.run(source, Interactives.print());
		System.out.println();
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		try {
			run(throwException(new NullPointerException()));
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
		
		run(singleton(1));
		
		run(range(0, 10));
		
		run(concat(singleton(0), range(1, 9)));
		
		run(where(range(0, 10), new Func2<Boolean, Integer, Integer>() {
			@Override
			public Boolean invoke(Integer param1, Integer param2) {
				System.out.printf("[%d]", param1);
				return param2 % 2 == 0;
			}
		}));
		
		run(selectMany(range(0, 10), new Func1<Iterable<Integer>, Integer>() {
			@Override
			public Iterable<Integer> invoke(Integer param1) {
				return range(0, param1 + 1);
			}
		}));
		
		run(distinctSet(
				concat(
						range(0, 10), 
						range(5, 10)), 
				identity(), 
				identity()));
		
		Iterable<GroupedIterable<Boolean, Integer>> ie = groupBy(range(0, 10),
			new Func1<Boolean, Integer>() {
				@Override
				public Boolean invoke(Integer param1) {
					return param1 % 2 == 0;
				}
			},
			Functions.<Integer>identity()
		);
		for (GroupedIterable<Boolean, Integer> g : ie) {
			System.out.print(g.key());
			run(g);
		}
		
		Iterable<Integer> it = distinctSet(Arrays.asList(1, 2, 3, 2, 1));
		
		run(it);
		run(it);
		
		run(generate(0, Functions.lessThan(10), incrementInt()));
		
		run(materialize(concat(range(0, 10), throwException(new RuntimeException()))));

		run(materialize(generate(0, lessThan(10), incrementInt())));

		try {
			Iterable<Option<Object>> materialize = materialize(concat(range(0, 10), throwException(new RuntimeException())));
			run(dematerialize(materialize));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
//		run(merge(range(0, 10), range(1000, 10)));
		
		run(merge(generate(0, lessThan(10), incrementInt(), 5, 1, TimeUnit.SECONDS), generate(10, lessThan(20), incrementInt(), 4, 1, TimeUnit.SECONDS)));
		
		System.out.printf("%nMain finished%n");
	}

}
