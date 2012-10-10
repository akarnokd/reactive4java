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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class Test3 {

	/**
	 * Utility class.
	 */
	private Test3() {
		// utility class
	}

	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {

		Observable<Timestamped<Integer>> tss = Reactive.generateTimed(0, Functions.lessThan(10), Functions.incrementInt(), 
				Functions.<Integer>identity(), Functions.<Long, Integer>constant(1000L));
		
		Observable<Timestamped<Integer>> tss2 = Reactive.generateTimed(10, Functions.lessThan(20), Functions.incrementInt(), 
				Functions.<Integer>identity(), Functions.<Long, Integer>constant(1000L));
		
		Observable<GroupedObservable<Integer, Integer>> groups = Reactive.groupBy(
				
				Reactive.select(Reactive.concat(tss, tss), Reactive.<Integer>unwrapTimestamped())
				, Functions.<Integer>identity())
				;
		
		groups.register(new Observer<GroupedObservable<Integer, Integer>>() {
			@Override
			public void next(
					final GroupedObservable<Integer, Integer> value) {
				//System.out.println("New group: " + value.key());
				value.register(new Observer<Integer>() {
					int count = 0;
					@Override
					public void next(Integer x) {
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
		
		AtomicBoolean sw = new AtomicBoolean(true);
		Reactive.ifThen(Functions.atomicSource(sw), tss, tss2).register(Reactive.println());
		Thread.sleep(3000);
		sw.set(false);
		
		
	}

}
