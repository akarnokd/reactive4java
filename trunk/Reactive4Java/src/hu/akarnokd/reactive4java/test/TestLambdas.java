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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Lambdas;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;


/**
 * Test various Interactive operators, 1.
 * @author akarnokd
 */
public final class TestLambdas {

	/**
	 * Utility class.
	 */
	private TestLambdas() {
		// utility class
	}
	/** 
	 * Run the Iterable with a print attached. 
	 * @param source the iterable source
	 * waiting on the observable completion
	 */
	static void run(Iterable<?> source) {
		try {
			Interactive.run(source, Interactive.print());
			System.out.println();
		} catch (Throwable t) {
			System.err.print(", ");
			t.printStackTrace();
		}
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
	public static void main(String[] args) throws Exception {
		
		run(Reactive.where(
				Reactive.range(0, 10), 
				Lambdas.<Integer, Boolean>js1("o => o % 2 == 0")));
		
		long t = 0;
		
		t = System.nanoTime();
		Action0 a = Lambdas.as0("=> var sum = 0; for (var i = 0; i < 10000000; i++) { sum += i; }");
		a.invoke();
		System.out.printf("Script: %d ns%n", (System.nanoTime() - t));
	
		t = System.nanoTime();
		long sum = 0;
		for (long i = 0; i < 10000000L; i++) {
			sum += i;
		}
		System.out.printf("Direct: %d ns%n", (System.nanoTime() - t));

		
		System.out.printf("%nMain finished%n");
	}

}
