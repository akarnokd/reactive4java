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

import hu.akarnokd.reactive4java8.base.IndexedPredicate;
import hu.akarnokd.reactive4java8.base.NewThreadScheduler;
import hu.akarnokd.reactive4java8.base.Observable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestTick {
    @Test(timeout = 1000)
    public void testSimpleTick() throws Exception {
        Observable<Long> timer = Observable.tick(100, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        List<Long> result = new LinkedList<>();
        timer.forEachWhile((i, v) -> {
            result.add(v);
            return i < 5;
        });
        TestUtil.assertEquals(result, 0L, 1L, 2L, 3L, 4L, 5L);
    }
    @Test(timeout = 1000)
    public void testRangedTick() {
        Observable<Long> result = Observable.tick(0, 3, 100, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        
        TestUtil.assertEquals(result, 0L, 1L, 2L);
    }
}
