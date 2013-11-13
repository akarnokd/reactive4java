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
import hu.akarnokd.reactive4java8.base.Scheduler;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestCombineLatest {
    @Test/*(timeout = 1000)*/
    public void testSimpleCombine() {
        Scheduler sch = new NewThreadScheduler();
        Observable<Long> src1 = Observable.tick(1, 3, 100, TimeUnit.MILLISECONDS, sch);
        Observable<Long> src2 = Observable.tick(1, 3, 50, 100, TimeUnit.MILLISECONDS, sch);
        
        Observable<Long> result = Observable.combineLatest(
                src1, src2, (t, u) -> t * u);
        
        TestUtil.assertEquals(result, 1L, 2L, 4L, 6L);
    }
}
