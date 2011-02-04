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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observables;

/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class Test5 {

	/**
	 * Utility class.
	 */
	private Test5() {
		// utility class
	}

	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {

		Observables.run(Observables.accumulate(Observables.range(1, 5),
				new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1, Integer param2) {
				return param1 + param2;
			}
		}), Observables.print());
		
		Observables.run(Observables.accumulate(Observables.range(1, 5), 1,
				new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1, Integer param2) {
				return param1 + param2;
			}
		}), Observables.print());
		
		Observables.run(Observables.accumulate0(Observables.range(1, 5), 1,
				new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1, Integer param2) {
				return param1 + param2;
			}
		}), Observables.print());
		
		Observables.run(Observables.select(Observables.range(1, 5), new Func2<Double, Integer, Integer>() {
				@Override
				public Double invoke(Integer param1, Integer param2) {
					return param1 * 0.5;
				}
		}), Observables.print());
		
		Observables.run(Observables.selectMany(Observables.range(1, 5), 
				new Func1<Observable<Integer>, Integer>() {
			@Override
			public Observable<Integer> invoke(Integer param1) {
				return Observables.range(0, param1);
			}
			
		}), Observables.print());
		
		Observables.run(Observables.selectMany(Observables.range(1, 5), Observables.range(5, 5))
				, Observables.print());
		
		Observables.run(Observables.selectMany(
		Observables.range(1, 10), 
		new Func1<Observable<Integer>, Integer>() {
			@Override
			public Observable<Integer> invoke(Integer param1) {
				return Observables.range(0, param1);
			}
			
		},
		new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1, Integer param2) {
				return param2 % param1;
			}
		}
		), Observables.print());
		
		
		Observables.run(Observables.skipLast(Observables.range(0, 10), 2), Observables.print());
	}

}
