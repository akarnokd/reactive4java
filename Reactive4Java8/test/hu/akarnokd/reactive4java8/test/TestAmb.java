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
public class TestAmb {
    @Test(timeout = 500)
    public void testSimpleAmb() {
        Observable<Long> first = Observable.tick(1, 2, 100, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        Observable<Long> second = Observable.tick(2, 10, 200, TimeUnit.MILLISECONDS, new NewThreadScheduler());
        
        Observable<Long> result = Observable.ambiguous(first, second);
        
        TestUtil.assertEquals(result, 1L, 2L);
    }
}
