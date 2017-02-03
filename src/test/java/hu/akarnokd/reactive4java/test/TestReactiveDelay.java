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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Timestamped;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Observers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test the Reactive.delay operators.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public class TestReactiveDelay {
	
	/**
	 * Test delayed registration and value delivery through observable sequences.
	 * @throws Exception ignored
	 */
	@Test
	public void testObservableDelays() throws Exception {
		Observable<Long> source = Reactive.tick(0, 5, 1000, TimeUnit.MILLISECONDS);
//		Observable<Long> source = Reactive.range(0L, 5L, new NewThreadScheduler());
		
		Observable<Long> regDelay = Reactive.tick(0, 1, 500, TimeUnit.MILLISECONDS);
		
		Func1<Long, Observable<Long>> valueDelay = new Func1<Long, Observable<Long>>() {
			@Override
			public Observable<Long> invoke(Long param1) {
//				return Reactive.tick(0, 1, (int)(param1 * 1000 + 500), TimeUnit.MILLISECONDS);
				return Reactive.tick(0, 1, 1000, TimeUnit.MILLISECONDS);
			}
		};
		
		Observable<Long> result = Reactive.delay(source, regDelay, valueDelay);
		
		
		Timestamped<Long> t = Timestamped.of(-1L);
		System.out.println(t);
		Reactive.run(Reactive.addTimestamped(result), Observers.println());
		System.out.println();
		
		Reactive.run(Reactive.addTimeInterval(result), Observers.println());

		TestUtil.assertEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L), result);

	}

}
