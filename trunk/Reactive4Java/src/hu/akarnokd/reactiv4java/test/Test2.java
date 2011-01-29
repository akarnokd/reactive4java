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

import hu.akarnokd.reactiv4java.Observable;
import hu.akarnokd.reactiv4java.Observables;

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

		System.out.println(Observables.first(Observables.range(1, 1)));
//		System.out.println(Observables.first(Observables.range(2, 0)));
		
		List<Observable<Integer>> list = new ArrayList<Observable<Integer>>();
		list.add(Observables.range(0, 10));
		list.add(Observables.range(10, 10));
		Observables.concat(list).register(Observables.printlnObserver());
		
		Observables.forkJoin(list).register(Observables.printlnObserver());
	}

}
