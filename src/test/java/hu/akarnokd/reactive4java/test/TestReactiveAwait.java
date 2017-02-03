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

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the Reactive.await operator.
 * @author akarnokd, 2013.01.12.
 */
public class TestReactiveAwait {
    /** Test a value. */
    @Test(timeout = 2500)
    public void simpleHasValue() {
        Observable<Long> source = Reactive.tick(0, 2, 1, TimeUnit.SECONDS);
        
        Long result = Reactive.await(source);
        
        Assert.assertEquals((Long)1L, result);
    }
    /** Test an empty sequence. */
    @Test(timeout = 1500, expected = NoSuchElementException.class)
    public void simpleEmpty() {
        Observable<Long> source = Reactive.<Long>empty();
        Observable<Long> delayed = Reactive.delay(source, 1, TimeUnit.SECONDS);
        
        Reactive.await(delayed);
    }
    /**
     * Test for an error in the sequence.
     */
    @Test(timeout = 1500)
    public void simpleException() {
        IOException ex = new IOException();
        Observable<Long> source = Reactive.<Long>throwException(ex);
        Observable<Long> delayed = Reactive.delay(source, 1, TimeUnit.SECONDS);
        
        try {
            Reactive.await(delayed);
        } catch (RuntimeException exc) {
            Assert.assertEquals(ex, exc.getCause());
        }
    }
    /** Test for a timeout case. */
    @Test/* (timeout = 1500)*/
    public void simpleTimeout() {
        Observable<Long> source = Reactive.tick(0, 300, 1, TimeUnit.SECONDS);
        
        try {
            Reactive.await(source, 500, TimeUnit.MILLISECONDS);
        } catch (RuntimeException exc) {
            TestUtil.assertInstanceof(TimeoutException.class, exc.getCause());
        }
    }
}
