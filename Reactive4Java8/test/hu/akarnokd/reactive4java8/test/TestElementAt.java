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

import hu.akarnokd.reactive4java8.base.NewThreadScheduler;
import hu.akarnokd.reactive4java8.base.Observable;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestElementAt {
    @Test
    public void simpleFound() {
        Observable<Integer> src = Observable.from(1, 2, 3, 4);
        Observable<Integer> r = src.elementAt(1);
        TestUtil.assertEquals(r, 2);
    }
    @Test
    public void simpleNotFound() {
        Observable<Integer> src = Observable.from(1, 2, 3, 4);
        Observable<Integer> r = src.elementAt(5);
        TestUtil.assertEquals(r);
    }
    @Test(timeout = 1000)
    public void simpleFoundTimed() {
        Observable<Long> src = Observable.tick(0, 3, 400, 
                TimeUnit.MILLISECONDS, new NewThreadScheduler());
        Observable<Long> r = src.elementAt(1);
        TestUtil.assertEquals(r, 1L);
    }
}
