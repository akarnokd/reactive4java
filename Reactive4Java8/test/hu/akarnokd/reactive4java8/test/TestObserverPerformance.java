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

import hu.akarnokd.reactive4java8.observers.AtomicObserver;
import hu.akarnokd.reactive4java8.observers.DefaultObserver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/**
 *
 * @author akarnokd
 */
public class TestObserverPerformance {
    @Test
    public void measure() throws Exception {
        ExecutorService exec1 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        int n = 1_000_000;
        
        AtomicLong inc = new AtomicLong();
        
        AtomicObserver<Long> ao = new AtomicObserver<Long>() {
            @Override
            public void onNext(Long value) {
                inc.incrementAndGet();
            }
            @Override
            public void onError(Throwable t) {
            }
            @Override
            public void onFinish() {
            }
        };

        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            exec1.execute(() -> ao.next(1L));
        }
        exec1.shutdown();
        exec1.awaitTermination(1, TimeUnit.DAYS);
        
        t0 = System.nanoTime() - t0;

        System.out.println(t0 / 1000000d);

        ExecutorService exec2 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        
        AtomicLong inc2 = new AtomicLong();

        DefaultObserver<Long> obs2 = new DefaultObserver<Long>() {
            @Override
            protected void onNext(Long value) {
                inc2.incrementAndGet();
            }
            @Override
            protected void onError(Throwable t) {
            }
            @Override
            protected void onFinish() {
            }
        };

        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            exec2.execute(() -> obs2.next(1L));
        }
        exec2.shutdown();
        exec2.awaitTermination(1, TimeUnit.DAYS);
        
        t1 = System.nanoTime() - t1;

        System.out.println(t1 / 1000000d);

    }
}