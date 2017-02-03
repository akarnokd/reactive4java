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

import hu.akarnokd.reactive4java.base.Option;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the Option correctness.
 * @author akarnokd, 2013.01.12.
 */
public class TestOption {
    /** Test some. */
    @Test
    public void testSome() {
        Option<Integer> o = Option.some(1);
        
        Assert.assertTrue(o.hasValue());
        Assert.assertFalse(o.hasError());
        Assert.assertFalse(o.isNone());
        
        Assert.assertSame(1, o.value());
    }
    /** Thest error. */
    @Test(expected = RuntimeException.class)
    public void testError() {
        RuntimeException ex = new RuntimeException();
        
        Option<Integer> o = Option.error(ex);
        
        Assert.assertFalse(o.hasValue());
        Assert.assertTrue(o.hasError());
        Assert.assertFalse(o.isNone());

        Assert.assertSame(ex, Option.getError(o));
        
        o.value();
    }
    /** Thest error. */
    @Test(expected = UnsupportedOperationException.class)
    public void testNone() {
        Option<Integer> o = Option.none();
        
        Assert.assertFalse(o.hasValue());
        Assert.assertFalse(o.hasError());
        Assert.assertTrue(o.isNone());

        o.value();
    }
}
