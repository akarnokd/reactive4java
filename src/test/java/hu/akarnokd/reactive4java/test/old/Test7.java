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

package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Functions;
import hu.akarnokd.reactive4java.util.Observers;

import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, 7.
 * @author akarnokd
 */
public final class Test7 {

    /**
     * Utility class.
     */
    private Test7() {
        // utility class
    }
    /** 
     * Run the observable with a print attached. 
     * @param observable the source observable
     * @throws InterruptedException when the current thread is interrupted while
     * waiting on the observable completion
     */
    static void run(Observable<?> observable) throws InterruptedException {
        Reactive.run(observable, Observers.print());
    }
    
    /**
     * @param args no arguments
     * @throws Exception on error
     */
    public static void main(String[] args) throws Exception {
        run(
            Reactive.takeUntil(
                Reactive.tick(1, TimeUnit.SECONDS),
                Reactive.tick(5, TimeUnit.SECONDS)
            )
        );

        Observable<Long> o = Reactive.take(
                Reactive.tick(1, TimeUnit.SECONDS),
                10
            );

        run(o);
        run(o);
        
        
        
        run(Reactive.takeWhile(Reactive.tick(1, TimeUnit.SECONDS), Functions.lessThan(5L)));
        
        
        run(
            Reactive.addTimestamped(
                Reactive.throttle(
                    Reactive.concat(
                        Reactive.tick(0, 10, 200, TimeUnit.MILLISECONDS),
                        Reactive.tick(10, 15, 1, TimeUnit.SECONDS)
                    ),
                500, TimeUnit.MILLISECONDS)
            )
        );
        
        System.out.printf("%nMain finished%n");
    }

}
