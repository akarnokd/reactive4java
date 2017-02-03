/*
 * Copyright 2011-2013 David Karnok
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

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Observers;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Test Reactive operators, 1.
 * @author akarnokd
 */
public final class Test {

	/**
	 * Utility class.
	 */
	private Test() {
		// utility class
	}

	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		for (int i : FunctionIterable.asIterable(FunctionIterable.single(42))) {
			System.out.println(i);
		}
		Iterator<Integer> it = FunctionIterable.asIterable(FunctionIterable.single(42)).iterator();
		try {
			System.out.println(it.next());
			System.out.println(it.next());
		} catch (NoSuchElementException ex) {
			ex.printStackTrace();
		}
		Func0<Func0<Option<Integer>>> xs = FunctionIterable.asFIterable(Interactive.range(0, 10));
		Func0<Option<Integer>> xse = xs.invoke();
		Option<Integer> x = null;
		while ((x = xse.invoke()) != Option.<Integer>none()) {
			System.out.println(x.value());
		}
		
		Iterable<Integer> ys = FunctionIterable.asIterable(xs);
		for (Integer y : ys) {
			System.out.println(y);
		}
		
		final CountDownLatch latch = new CountDownLatch(1); 
		Action1<Action1<Option<Integer>>> oxs = ActionObservable.asFObservable(Reactive.range(20, 10));
		
		oxs.invoke(new Action1<Option<Integer>>() {
			@Override
			public void invoke(Option<Integer> value) {
				if (value != Option.<Integer>none()) {
					System.out.println(value.value());
				} else {
					latch.countDown();
				}
			}
		});
		
		latch.await();
		
		Observable<Integer> oys = ActionObservable.asObservable(oxs);
		oys.register(new Observer<Integer>() {
			@Override
			public void finish() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void next(Integer value) {
				System.out.println(value * 10);
			}
		});
		
		Reactive.toObservable(ys).register(Observers.toObserver(new Action1<Integer>() {
			@Override
			public void invoke(Integer value) {
				System.out.printf("%s %d%n", Thread.currentThread(), value);
			}
		}));
		
		Reactive.delay(Reactive.range(100, 10), 5, TimeUnit.SECONDS).register(Observers.println());

		System.out.println(Reactive.first(Reactive.range(1, 1)));
		
		System.out.println(Reactive.first(Reactive.range(2, 0)));
	}

}
