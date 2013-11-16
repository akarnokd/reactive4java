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

import hu.akarnokd.reactive4java8.base.Observer;
import hu.akarnokd.reactive4java8.base.ReplaySubject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author akarnokd
 */
public class TestReplaySubject {
    @Test
    public void simpleReplay() {
        ReplaySubject<Integer> rs = new ReplaySubject<>();
        rs.next(1);
        rs.next(2);
        rs.finish();
        
        TestUtil.assertEquals(rs, 1, 2);
        
        rs.next(3);

        TestUtil.assertEquals(rs, 1, 2);
    }
    @Test
    public void mixedReplay() {
        ReplaySubject<Integer> rs = new ReplaySubject<>();
        rs.next(1);
        rs.next(2);

        List<Integer> list1 = new ArrayList<>();
        rs.register(list1::add);
        
        rs.next(3);

        List<Integer> list2 = new ArrayList<>();
        rs.register(list2::add);
        
        rs.finish();
        
        TestUtil.assertEquals(list1, 1, 2, 3);
        TestUtil.assertEquals(list2, 1, 2, 3);
    }
}
