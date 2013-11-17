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

package hu.akarnokd.reactive4java8.schedulers;

import hu.akarnokd.reactive4java8.Scheduler;
import hu.akarnokd.reactive4java8.Registration;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that start a new thread for each submitted task.
 * @author karnok
 */
public class NewThreadScheduler implements Scheduler {
    /**
     * Creates and starts a thread with the given task.
     * @param task
     * @return 
     */
    protected Registration createThread(Runnable task) {
        Thread t = new Thread(task);
        t.start();
        return () -> { t.interrupt(); };
    }

    @Override
    public Registration schedule(Runnable run) {
        return createThread(run);
    }

    @Override
    public Registration schedule(long time, TimeUnit unit, Runnable run) {
        return createThread(() -> {
            try {
                unit.sleep(time);
                run.run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public Registration schedule(long initialDelay, long period, TimeUnit unit, Runnable run) {
        return createThread(() -> {
            try {
                unit.sleep(initialDelay);

                long t0 = System.nanoTime();

                while (!Thread.currentThread().isInterrupted()) {
                    t0 += unit.toNanos(period);
                    
                    run.run();
                    
                    long toWait = t0 - System.nanoTime();
                    if (toWait > 0) {
                        TimeUnit.NANOSECONDS.sleep(toWait);
                    }
                    
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
