/*
 * Copyright 2011-2012 David Karnok
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

package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Pred1;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;


/**
 * Test Reactive operators, B.
 * @author akarnokd
 */
public final class TestE {

	/**
	 * Utility class.
	 */
	private TestE() {
		// utility class
	}
	/** 
	 * Run the observable with a print attached. 
	 * @param observable the source observable
	 * @throws InterruptedException when the current thread is interrupted while
	 * waiting on the observable completion
	 */
	static void run(Observable<?> observable) throws InterruptedException {
		Reactive.run(observable, Reactive.print());
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args)
	throws Exception {
		
		// Issue #1
		
		Iterable<Integer> i1 = Interactive.toIterable(1, 3, 5);
		Interactive.run(i1, Interactive.print());
		System.out.println();
		
		Iterable<Integer> i2 = Interactive.generate(1, new Pred1<Integer>() {
			@Override
			public Boolean invoke(Integer param1) {
				return param1 < 1 + 3 * 2;
			}
		}, new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 + 2;
			}
		});
		Interactive.run(i2, Interactive.print());
		System.out.println();
		
		// Issue #2
		System.out.println();
		
		Iterable<Integer> i3 = Interactive.toIterable(1, 2);
		Interactive.run(i3, Interactive.print());
		System.out.println();
		Iterable<Integer> i4 = Interactive.concat(i3, Interactive.toIterable(3, 4));
		Interactive.run(i4, Interactive.print());
		System.out.println();
		
		Iterable<Integer> i5 = Interactive.take(i4, 2);
		Interactive.run(i5, Interactive.print());
		System.out.println();
		
		
		System.out.printf("%nMain finished%n");
	}

}
