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
import static hu.akarnokd.reactive4java.reactive.Reactive.all;
import static hu.akarnokd.reactive4java.util.Functions.equal;

import org.junit.Test;

/**
 * Test the Reactive.all operators.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveAll {

    /**
     * Tests all() properly returning <code>false</code>.
     */
    @Test
    public void allFalse() {
        int value = 42;
        TestUtil.assertSingle(false, all(from(value, 0, value), equal(value)));
    }

    /**
     * Tests all() properly returning <code>true</code>.
     */
    @Test
    public void allTrue() {
        int value = 42;
        TestUtil.assertSingle(true, all(from(value, value, value), equal(value)));
    }

}
