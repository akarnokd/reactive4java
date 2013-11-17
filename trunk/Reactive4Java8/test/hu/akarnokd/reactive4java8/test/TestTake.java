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
public class TestTake {
    @Test
    public void takeSome() {
        Observable<Integer> src = Observable.from(0, 1, 2, 3);
        Observable<Integer> r = src.take(2);
        
        TestUtil.assertEquals(r, 0, 1);
    }
    @Test
    public void takeMore() {
        Observable<Integer> src = Observable.from(0, 1, 2, 3);
        Observable<Integer> r = src.take(6);
        
        TestUtil.assertEquals(r, 0, 1, 2, 3);
    }
    @Test(timeout = 500)
    public void takeNone() {
        Observable<Long> src = Observable.tick(0, 5, 1000, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        Observable<Long> r = src.take(0);
        
        TestUtil.assertEquals(r);
    }
}
