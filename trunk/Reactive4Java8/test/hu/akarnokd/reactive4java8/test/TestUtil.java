/*
 * Copyright 2013 karnok.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactive4java8.test;

import hu.akarnokd.reactive4java8.Observable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import junit.framework.Assert;

/**
 *
 * @author karnok
 */
public class TestUtil {
    /**
     * Test if the observable produces the expected sequence.
     * @param <T>
     * @param actual
     * @param expected 
     */
    @SafeVarargs
    public static <T> void assertEquals(Observable<T> actual, T... expected) {
        List<T> act = new LinkedList<>();
        actual.forEach(v -> act.add(v));
        Assert.assertEquals(Arrays.asList(expected), act);
    }
    /**
     * Test if the iterable sequence produces the expected sequence.
     * @param <T>
     * @param actual
     * @param expected 
     */
    @SafeVarargs
    public static <T> void assertEquals(Iterable<T> actual, T... expected) {
        List<T> act = new LinkedList<>();
        actual.forEach(v -> act.add(v));
        Assert.assertEquals(Arrays.asList(expected), act);
    }
}
