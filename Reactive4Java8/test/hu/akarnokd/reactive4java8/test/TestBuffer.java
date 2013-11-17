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
import java.util.List;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestBuffer {
    @Test
    public void testSimpleBuffer() {
        Observable<Integer> src = Observable.range(0, 4);
        Observable<List<Integer>> result = src.buffer(2);
        
        TestUtil.assertEquals(result, Arrays.asList(0, 1), Arrays.asList(2, 3));
    }
}
