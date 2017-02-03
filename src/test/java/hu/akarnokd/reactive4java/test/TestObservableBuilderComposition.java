/*
 * Copyright 2011-2013 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Pred1;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test the observable builder's composition behavior.
 * @author akarnokd, 2013.01.10.
 */
public class TestObservableBuilderComposition {
    /** Test a chain of where and select. */
    @Test
    public void test() {
        Observable<Integer> source = Reactive.range(0, 10);
        
        Observable<Integer> result = ObservableBuilder
                .from(source)
                .where(new Pred1<Integer>() { 
                    @Override
                    public Boolean invoke(Integer param1) {
                        return param1 % 2 == 0;
                    }
                })
                .select(new Func1<Integer, Integer>() { 
                    @Override
                    public Integer invoke(Integer param1) {
                        return param1 * param1;
                    }
                });
        
        TestUtil.assertEqual(Arrays.asList(0, 4, 16, 36, 64), result);
    }

}
