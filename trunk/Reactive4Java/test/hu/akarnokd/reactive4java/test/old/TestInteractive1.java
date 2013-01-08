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
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.interactive.Interactive;


/**
 * Test various Interactive operators, 1.
 * @author akarnokd
 */
public final class TestInteractive1 {

	/**
	 * Utility class.
	 */
	private TestInteractive1() {
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
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		
		run(Interactive.skipLast(Interactive.range(0, 10), 3));
		
		run(Interactive.takeLast(Interactive.range(0, 10), 3));
		
		run(Interactive.zip(Interactive.range(0, 10), 
				Interactive.range(0, 5), 
				new Func2<Integer, Integer, String>() {
			@Override
			public String invoke(Integer param1, Integer param2) {
				return param1 + ":" + param2;
			}
		}));
		
		run(Interactive.max(Interactive.range(0, 10)));
		
	    Interactive.run(
	    		Interactive.orderBy(
		            Interactive.selectMany(
		                Interactive.range(0, 10), 
		                new Func1<Integer, Iterable<Integer>>() {
		                	@Override
		                    public Iterable<Integer> invoke(Integer param1) {
		                       return Interactive.range(0, param1);
		                }
		            }
		        ),
	            Functions.<Integer>comparatorReverse()
	        ), Interactive.println());
		
		System.out.printf("%nMain finished%n");
	}

}
