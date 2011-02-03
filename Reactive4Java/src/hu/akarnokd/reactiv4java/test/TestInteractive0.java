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

package hu.akarnokd.reactiv4java.test;

import java.util.Arrays;

import hu.akarnokd.reactiv4java.Func1;
import hu.akarnokd.reactiv4java.Func2;
import hu.akarnokd.reactiv4java.Functions;
import hu.akarnokd.reactiv4java.GroupedIterable;
import hu.akarnokd.reactiv4java.Interactives;


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
			run(Interactives.throwException(new NullPointerException()));
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
		
		run(Interactives.singleton(1));
		
		run(Interactives.range(0, 10));
		
		run(Interactives.concat(Interactives.singleton(0), Interactives.range(1, 9)));
		
		run(Interactives.where(Interactives.range(0, 10), new Func2<Boolean, Integer, Integer>() {
			@Override
			public Boolean invoke(Integer param1, Integer param2) {
				System.out.printf("[%d]", param1);
				return param2 % 2 == 0;
			}
		}));
		
		run(Interactives.selectMany(Interactives.range(0, 10), new Func1<Iterable<Integer>, Integer>() {
			@Override
			public Iterable<Integer> invoke(Integer param1) {
				return Interactives.range(0, param1 + 1);
			}
		}));
		
		run(Interactives.distinctSet(
				Interactives.concat(
						Interactives.range(0, 10), 
						Interactives.range(5, 10)), 
				Functions.identity(), 
				Functions.identity()));
		
		Iterable<GroupedIterable<Boolean, Integer>> ie = Interactives.groupBy(Interactives.range(0, 10),
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
		
		Iterable<Integer> it = Interactives.distinctSet(Arrays.asList(1, 2, 3, 2, 1));
		
		run(it);
		run(it);
		
		run(Interactives.generate(0, Functions.lessThan(10), Functions.incrementInt()));
		
		System.out.printf("%nMain finished%n");
	}

}
