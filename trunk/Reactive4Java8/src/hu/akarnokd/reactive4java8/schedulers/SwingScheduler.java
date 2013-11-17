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

package hu.akarnokd.reactive4java8.schedulers;

import hu.akarnokd.reactive4java8.Scheduler;
import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.util.LockSync;
import hu.akarnokd.reactive4java8.util.BoolRef;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Scheduler that executes tasks on the Swing Event Thread.
 * @author akarnokd, 2013.11.16.
 */
public class SwingScheduler implements Scheduler {

    @Override
    public Registration schedule(Runnable run) {
        FutureTask<?> task = new FutureTask<>(run, null);
        SwingUtilities.invokeLater(task);
        return () -> task.cancel(true);
    }

    @Override
    public Registration schedule(long time, TimeUnit unit, Runnable run) {
        Timer timer = new Timer((int)unit.toMillis(time), (a) -> run.run());
        timer.start();
        return () -> timer.stop();
    }

    @Override
    public Registration schedule(long initialDelay, long period, TimeUnit unit, Runnable run) {
        Timer timer = new Timer((int)unit.toMillis(initialDelay), null);
        LockSync ls = new LockSync();
        BoolRef cancel = new BoolRef();
        
        timer.addActionListener((a) -> {
            timer.stop();
            if (!ls.sync(() -> cancel.value)) {
                long t0 = System.currentTimeMillis();
                
                run.run();
                
                long dt = System.currentTimeMillis() - t0;
                int delay = (int)(unit.toMillis(period) - dt);
                
                ls.sync(() -> {
                    if (!cancel.value) {
                        timer.setDelay(delay);
                        timer.start();
                    }
                });
            }
        });
        
        
        timer.start();
        return () -> {
            ls.sync(() -> cancel.value = true);
            timer.stop();
        };
    }

}
