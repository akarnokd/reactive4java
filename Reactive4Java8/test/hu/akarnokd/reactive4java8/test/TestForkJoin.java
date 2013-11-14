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

import hu.akarnokd.reactive4java8.base.Observable;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author karnok
 */
public class TestForkJoin {
    @Test
    public void simpleForkJoin() {
        Observable<Integer> src1 = Observable.from(0);
        Observable<Integer> src2 = Observable.from(1, 2);
        
        Observable<List<Integer>> r = Observable.forkJoin(src1, src2);
        
        TestUtil.assertEquals(r, Arrays.asList(0, 2));
    }
}
