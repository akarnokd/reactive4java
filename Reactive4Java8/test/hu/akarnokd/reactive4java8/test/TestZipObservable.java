/*
 * Copyright 2013 akarnokd.
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

import hu.akarnokd.reactive4java8.base.Observable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author akarnokd
 */
public class TestZipObservable {
    @Test
    public void testSameSize() {
        Observable<Integer> o1 = Observable.from(1, 2, 3);
        Observable<Integer> o2 = Observable.from(1, 2, 3);
        
        List<Integer> list = new ArrayList<>();
        
        o1.zip(o2, (t, u) -> t - u).forEach(v -> list.add(v));
        
        Assert.assertEquals(Arrays.asList(0, 0, 0), list);
    }
    public void testDifferentSize1() {
        Observable<Integer> o1 = Observable.from(1, 2);
        Observable<Integer> o2 = Observable.from(1, 2, 3);
        
        List<Integer> list = new ArrayList<>();
        
        o1.zip(o2, (t, u) -> t - u).forEach(v -> list.add(v));
        
        Assert.assertEquals(Arrays.asList(0, 0), list);
    }
    public void testDifferentSize2() {
        Observable<Integer> o1 = Observable.from(1, 2, 3);
        Observable<Integer> o2 = Observable.from(1, 2);
        
        List<Integer> list = new ArrayList<>();
        
        o1.zip(o2, (t, u) -> t - u).forEach(v -> list.add(v));
        
        Assert.assertEquals(Arrays.asList(0, 0), list);
    }
}
