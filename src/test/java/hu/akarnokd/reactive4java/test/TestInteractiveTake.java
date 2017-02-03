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

import static hu.akarnokd.reactive4java.interactive.Interactive.concat;
import static hu.akarnokd.reactive4java.interactive.Interactive.size;
import static hu.akarnokd.reactive4java.interactive.Interactive.take;
import static hu.akarnokd.reactive4java.interactive.Interactive.toIterable;

import org.junit.Test;

/**
 * Test the Interactive.take operator.
 * @author Denes Harmath, 2012.07.13.
 */
public class TestInteractiveTake {
    /**
     * Test take().
     */
    @Test
    public void takeOk() {
        Iterable<Integer> prefix = toIterable(1, 2);
        Iterable<Integer> i = concat(prefix, toIterable(3, 4));
        TestUtil.assertEqual(take(i, size(prefix)), prefix);
    }

}
