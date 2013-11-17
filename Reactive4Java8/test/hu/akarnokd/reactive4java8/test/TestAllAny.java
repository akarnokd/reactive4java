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

import hu.akarnokd.reactive4java8.schedulers.NewThreadScheduler;
import hu.akarnokd.reactive4java8.Observable;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestAllAny {
    @Test
    public void testSimpleAll() {
        Observable<Integer> src = Observable.from(0, 2, 4, 6);
        
        Observable<Boolean> result = src.all(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, true);
    }
    @Test
    public void testSimpleAll2() {
        Observable<Integer> src = Observable.from(0, 1, 4, 6);
        
        Observable<Boolean> result = src.all(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, false);
    }
    @Test
    public void testSimpleAll3() {
        Observable<Integer> src = Observable.from();
        
        Observable<Boolean> result = src.all(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result);
    }
    @Test(timeout = 1000)
    public void testSimpleAllReturnEarly() {
        Observable<Long> src = Observable.tick(0, 5, 400, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        
        Observable<Boolean> result = src.all(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, false);
    }
    @Test
    public void testSimpleAny() {
        Observable<Integer> src = Observable.from(0, 2, 4, 6);
        
        Observable<Boolean> result = src.any(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, true);
    }
    @Test
    public void testSimpleAny2() {
        Observable<Integer> src = Observable.from(1, 3, 5, 7);
        
        Observable<Boolean> result = src.any(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, false);
    }
    @Test
    public void testSimpleAny3() {
        Observable<Integer> src = Observable.from();
        
        Observable<Boolean> result = src.any(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result);
    }
    @Test(timeout = 1000)
    public void testSimpleAnyReturnEarly() {
        Observable<Long> src = Observable.tick(1, 5, 400, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        
        Observable<Boolean> result = src.any(v -> v % 2 == 0);
        
        TestUtil.assertEquals(result, true);
    }
}
