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
		
		run(Interactives.singleton(1));
		
		run(Interactives.range(0, 10));
		
		run(Interactives.concat(Interactives.singleton(0), Interactives.range(1, 9)));
		
		try {
			run(Interactives.throwException(new NullPointerException()));
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
		
		System.out.printf("%nMain finished%n");
	}

}
