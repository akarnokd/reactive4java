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

import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.util.Functions.equal;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

/**
 * Test the Reactive.takeXXX operators.
 * @author Denes Harmath, 2012.07.16.
 * @since 0.97
 */
public class TestReactiveTake {
    /**
     * Tests take().
     */
    @Test
    public void takeOk() {
        Observable<Integer> prefix = from(1, 2);
        Observable<Integer> postfix = from(3, 4);
        Observable<Integer> o = Reactive.concat(prefix, postfix);
        Integer count = Reactive.single(Reactive.count(prefix));
        TestUtil.assertEqual(prefix, Reactive.take(o, count));
    }
    /**
     * Tests takeLast().
     */
    @Test
    @Ignore // FIXME bug in concat
    public void takeLastOk() {
        Observable<Integer> prefix = from(1, 2);
        Observable<Integer> postfix = from(3, 4);
        Observable<Integer> o = Reactive.concat(prefix, postfix);
        Integer count = Reactive.single(Reactive.count(postfix));
        TestUtil.assertEqual(postfix, Reactive.takeLast(o, count));
    }
    /**
     * Tests takeWhile() with some elements taken.
     */
    @Test
    public void takeWhileSome() {
        Integer value = 42;
        Observable<Integer> prefix = from(value, value);
        Observable<Integer> postfix = from(0, value);
        Observable<Integer> o = Reactive.concat(prefix, postfix);
        TestUtil.assertEqual(prefix, Reactive.takeWhile(o, equal(value)));
    }
    /**
     * Tests takeWhile() with all elements taken.
     */
    @Test
    public void takeWhileAll() {
        Integer value = 42;
        Observable<Integer> o = from(value, value);
        TestUtil.assertEqual(o, Reactive.takeWhile(o, equal(value)));
    }
    /**
     * Tests takeWhile() with no elements taken.
     */
    @Test
    public void takeWhileNone() {
        Integer value = 42;
        TestUtil.assertEqual(Reactive.empty(), Reactive.takeWhile(from(0, value), equal(value)));
    }
    /** TakeLast with time. */
    @Test(timeout = 1500)
    public void takeLastTimed() {
        Observable<Long> source = Reactive.tick(0, 10, 100, TimeUnit.MILLISECONDS);
        
        Observable<Long> result = Reactive.takeLast(source, 450, TimeUnit.MILLISECONDS);
        
        TestUtil.assertEqual(Arrays.asList(5L, 6L, 7L, 8L, 9L), result);
    }
    /** Test take with zer count. */
    @Test
    public void take0() {
        Observable<Long> numbers = Reactive.range(1L, 5L);
        
        Observable<Long> result = Reactive.take(numbers, 0);
        
        TestUtil.assertEqual(Collections.<Long>emptyList(), result);
    }
    /** 
     * Test if the take finishes once the requested
     * number of items were received.
     */
    @Test(timeout = 1500)
    public void takeFirstFinish() {
        Observable<Long> tick = Reactive.tick(0, 2, 1, TimeUnit.SECONDS);
        
        Observable<Long> result = Reactive.take(tick, 1);

        TestUtil.assertEqual(Arrays.asList(0L), result);
}
}

