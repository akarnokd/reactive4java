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

import static hu.akarnokd.reactive4java.base.Functions.identity;
import static hu.akarnokd.reactive4java.base.Functions.incrementInt;
import static hu.akarnokd.reactive4java.base.Functions.lessThan;
import static hu.akarnokd.reactive4java.interactive.Interactive.any;
import static hu.akarnokd.reactive4java.interactive.Interactive.catchException;
import static hu.akarnokd.reactive4java.interactive.Interactive.concat;
import static hu.akarnokd.reactive4java.interactive.Interactive.count;
import static hu.akarnokd.reactive4java.interactive.Interactive.dematerialize;
import static hu.akarnokd.reactive4java.interactive.Interactive.distinctSet;
import static hu.akarnokd.reactive4java.interactive.Interactive.generate;
import static hu.akarnokd.reactive4java.interactive.Interactive.groupBy;
import static hu.akarnokd.reactive4java.interactive.Interactive.materialize;
import static hu.akarnokd.reactive4java.interactive.Interactive.memoize;
import static hu.akarnokd.reactive4java.interactive.Interactive.memoizeAll;
import static hu.akarnokd.reactive4java.interactive.Interactive.merge;
import static hu.akarnokd.reactive4java.interactive.Interactive.publish;
import static hu.akarnokd.reactive4java.interactive.Interactive.range;
import static hu.akarnokd.reactive4java.interactive.Interactive.resumeAlways;
import static hu.akarnokd.reactive4java.interactive.Interactive.resumeOnError;
import static hu.akarnokd.reactive4java.interactive.Interactive.retry;
import static hu.akarnokd.reactive4java.interactive.Interactive.scan;
import static hu.akarnokd.reactive4java.interactive.Interactive.select;
import static hu.akarnokd.reactive4java.interactive.Interactive.selectMany;
import static hu.akarnokd.reactive4java.interactive.Interactive.singleton;
import static hu.akarnokd.reactive4java.interactive.Interactive.throwException;
import static hu.akarnokd.reactive4java.interactive.Interactive.where;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.interactive.GroupedIterable;
import hu.akarnokd.reactive4java.interactive.Interactive;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * Test various Interactive operators, 0.
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
		run(throwException(new NullPointerException()));
		
		run(singleton(1));
		
		run(range(0, 10));
		
		run(concat(singleton(0), range(1, 9)));
		
		run(where(range(0, 10), 
				new Func2<Integer, Integer, Boolean>() {
			@Override
			public Boolean invoke(Integer param1, Integer param2) {
				System.out.printf("[%d]", param1);
				return param2 % 2 == 0;
			}
		}));
		
		run(selectMany(range(0, 10), 
		new Func1<Integer, Iterable<Integer>>() {
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
			new Func1<Integer, Boolean>() {
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
		
		Iterable<Integer> gen0to10 = generate(0, Functions.lessThan(10), incrementInt());
		run(gen0to10);
		
		run(materialize(concat(range(0, 10), throwException(new RuntimeException()))));

		run(materialize(generate(0, lessThan(10), incrementInt())));

		final Iterable<Object> cc = concat(range(0, 10), throwException(new RuntimeException()));
		Iterable<Option<Object>> materialize = materialize(cc);
		run(dematerialize(materialize));
		
		run(merge(range(0, 10), range(1000, 10)));
		
		run(merge(generate(0, lessThan(10), incrementInt(), 5, 1, TimeUnit.SECONDS), throwException(new RuntimeException())));

		run(retry(cc, 4));
		
		run(count(cc));
		
		run(count(gen0to10));
		
		run(catchException(cc));
		
		System.out.println("Resume on error:");
		run(resumeOnError(cc, cc));
		
		System.out.println("Resume always:");
		run(resumeAlways(Arrays.<Iterable<?>>asList(gen0to10, cc, gen0to10, cc)));
		
		run(scan(gen0to10, Functions.sumInteger()));

		Iterable<Integer> random = select(range(0, 10), new Func1<Integer, Integer>() {
			Random r = new Random();
			@Override
			public Integer invoke(Integer param1) {
				return param1 + r.nextInt(10);
			}
		});
		
		run(random);
		run(random);
		
		Iterable<Integer> p = memoizeAll(random);
		
		run(p);
		run(p);
		
		System.out.println("Publish(I, F)");
		
		Func1<Iterable<Integer>, Iterable<Integer>> f = new Func1<Iterable<Integer>, Iterable<Integer>>() {
			@Override
			public Iterable<Integer> invoke(Iterable<Integer> param1) {
				System.out.printf("f%n%n");
				run(param1);
				return param1;
			}
		};

		Iterable<Integer> p1 = publish(random, f);
		run(p1);
		run(p1);

		Iterable<Integer> p2 = publish(range(10, 5), f);
		run(p2);
		run(p2);

		System.out.println("Memorize");
		
		Iterable<Integer> p3 = memoize(range(0, 3), 2);

		Iterator<Integer> p3i1 = p3.iterator();
		Iterator<Integer> p3i2 = p3.iterator();
		Iterator<Integer> p3i3 = p3.iterator();
		
		System.out.println(p3i1.next());
		System.out.println(p3i1.next());
		System.out.println(p3i2.next());
		System.out.println(p3i2.next());
		System.out.println(p3i3.next());
		System.out.println(p3i3.next());
		System.out.println(p3i1.next());
		System.out.println(p3i3.next());
		
		run(any(range(1, 10)));
		run(any(range(1, 10), Functions.equal(0)));
		
		System.out.printf("%nMain finished%n");
	}

}
