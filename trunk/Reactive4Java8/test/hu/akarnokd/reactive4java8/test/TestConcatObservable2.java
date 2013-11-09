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
public class TestConcatObservable2 {
    @Test(timeout = 1000)
    public void testSimpleCompletion() {
        List<Integer> result = new ArrayList<>();
        
        Observable<Integer> o1 = Observable.from(1, 2);
        Observable<Integer> o2 = Observable.from(3, 4);
        Observable<Integer> o3 = Observable.from(5, 6);
        Observable<Observable<Integer>> o = Observable.from(o1, o2, o3);
        
        Observable.concat(o).forEach((v) -> result.add(v));
        
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }
    @Test /* (timeout = 1000) */
    public void testSimpleConcatenation() {
        
        List<Integer> result = new ArrayList<>();

        Observable<Integer> o1 = Observable.from(1, 2);
        Observable<Integer> o2 = Observable.from(3, 4);
        Observable<Integer> o3 = Observable.from(5, 6);

        Observable.concat(o1, o2, o3).forEach((v) -> result.add(v));
        
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);

    }
}
