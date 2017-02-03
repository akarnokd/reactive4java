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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Functions;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Test the Reactive.elementAt operators.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveElementAt {
    /**
     * Test if the surely existing index returns the correct value.
     */
    @Test
    public void existing() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 5);
        
        TestUtil.assertEqual(Arrays.asList(5), result);
    }
    /**
     * Test if the surely exising index is returned in case of a supplied default.
     */
    @Test
    public void existingWithDefault() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 5, 10);
        
        TestUtil.assertEqual(Arrays.asList(5), result);
    }
    /**
     * Test if the surely exising index is returned in case of a supplied default supplier.
     */
    @Test
    public void existingWithDefaultFunction() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 5, Functions.constant0(10));
        
        TestUtil.assertEqual(Arrays.asList(5), result);
    }
    /**
     * Test if the nonexistent index throws the exception.
     */
    @Test(expected = NoSuchElementException.class)
    public void nonexistent() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 10);
        
        TestUtil.assertEqual(Arrays.asList(4), result);
    }
    /**
     * Test if the nonexistent index returns the default value.
     */
    @Test
    public void nonexistentDefault() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 10, 10);
        
        TestUtil.assertEqual(Arrays.asList(10), result);
    }
    /**
     * Test if the nonexistent index returns the default value.
     */
    @Test
    public void nonexistentDefaultFunction() {
        Observable<Integer> values = Reactive.range(0, 10);
        
        Observable<Integer> result = Reactive.elementAt(values, 10, Functions.constant0(10));
        
        TestUtil.assertEqual(Arrays.asList(10), result);
    }
}
