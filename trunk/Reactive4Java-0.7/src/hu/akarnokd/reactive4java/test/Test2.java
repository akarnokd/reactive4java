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

import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.reactive.GroupedObservable;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Timestamped;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class Test2 {

	/**
	 * Utility class.
	 */
	private Test2() {
		// utility class
	}

	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {

		System.out.println(Reactive.first(Reactive.range(1, 1)));
//		System.out.println(Observables.first(Observables.range(2, 0)));
		
		List<Observable<Integer>> list = new ArrayList<Observable<Integer>>();
		list.add(Reactive.range(0, 10));
		list.add(Reactive.range(10, 10));
		Reactive.concat(list).register(Reactive.println());
		
		Reactive.forkJoin(list).register(Reactive.println());
		
		Closeable c = Reactive.range(0, Integer.MAX_VALUE).register(Reactive.println());
		
		Thread.sleep(1000);
		
		c.close();
		
		Reactive.generateTimed(0, Functions.lessThan(10), Functions.incrementInt(), 
				Functions.<Integer>identity(), Functions.<Long, Integer>constant(1000L))
				.register(Reactive.println())
				;
		
		Observable<Timestamped<Integer>> tss = Reactive.generateTimed(0, Functions.lessThan(10), Functions.incrementInt(), 
				Functions.<Integer>identity(), Functions.<Long, Integer>constant(1000L));
		
		Observable<GroupedObservable<Timestamped<Integer>, Timestamped<Integer>>> groups = Reactive.groupBy(
				
				Reactive.concat(tss, tss)
				, Functions.<Timestamped<Integer>>identity())
				;
		
		groups.register(new Observer<GroupedObservable<Timestamped<Integer>, Timestamped<Integer>>>() {
			@Override
			public void next(
					final GroupedObservable<Timestamped<Integer>, Timestamped<Integer>> value) {
				//System.out.println("New group: " + value.key());
				value.register(new Observer<Timestamped<Integer>>() {
					int count = 0;
					@Override
					public void next(Timestamped<Integer> x) {
						System.out.println("Group " + value.key() + ", Size " + (++count));
					}

					@Override
					public void error(Throwable ex) {
						
					}

					@Override
					public void finish() {
						
					}
					
				});
			}
			@Override
			public void error(Throwable ex) {
				
			}
			@Override
			public void finish() {
				
			}
		});
	}

}
