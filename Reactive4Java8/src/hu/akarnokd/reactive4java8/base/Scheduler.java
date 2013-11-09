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

package hu.akarnokd.reactive4java8.base;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Abstraction of a thread pool where tasks can be submitted.
 * <p>reactive4java notes:
 * <ul>
 * <li>It uses the Java 8 functional classes
 * instead of the custom ones.</li>
 * <li>In addition, the scheduler interface
 * is now closer to Rx's own scheduler which generally passes
 * around itself. The original simpler methods are
 * now implemented as default methods.</li>
 * <li>The time parameters of the schedule() methods are put at first
 * to better fit the lambda-as-last-parameter idiom.</p>
 * </ul>
 * </p>
 * @author akarnokd, 2013.11.08.
 */
public interface Scheduler {
    /**
     * Schedules the given activity.
     * @param <TState> the state information type to pass to the activity
     * @param state the state to pass to the activity
     * @param activity the activity
     * @return the registration to cancel the schedule
     */
    <TState> Registration schedule(TState state, 
            BiFunction<Scheduler, TState, Registration> activity);
    /**
     * Schedules the given activity to run after the given amount of time.
     * <p>This overload allows the chaining of scheduled activities by using
     * the same registration object.</p> 
     * @param <TState> the state information type to pass to the activity
     * @param state the state to pass to the activity
     * @param time the time to delay the execution of the activity
     * @param unit the time unit of the delay time
     * @param activity the activity
     * @return  the registration to cancel the schedule
     */
    <TState> Registration schedule(TState state, long time, TimeUnit unit, 
            BiFunction<Scheduler, TState, Registration> activity);
    /**
     * Schedules a periodic activity.
     * <p>Note: this method is in separate interface in the Rx, so it might
     * be moved into a separate one too.</p>
     * @param <TState> the state information type to pass to the activity
     * @param state the state to pass to the activity
     * @param initialDelay the initial delay
     * @param period the frequency to execute the activity
     * @param unit the time unit of the delay time
     * @param activity the activity
     * @return  the registration to cancel the schedule
     */
    <TState> Registration schedule(TState state, long initialDelay, long period, 
            TimeUnit unit, BiFunction<Scheduler, TState, Registration> activity);
    /**
     * Schedule a task to run once.
     * @param run the task to run
     * @return the registration to cancel the execution of the task
     */
    default Registration schedule(Runnable run) {
        return schedule(new Object(), (scheduler, state) -> { run.run(); return Registration.EMPTY; });
    }
    /**
     * Schedule a task to run after the given time.
     * @param time the time to delay the execution of the activity
     * @param unit the time unit of the delay time
     * @param run the task to run
     * @return the registration to cancel the execution of the task
     */
    default Registration schedule(long time, TimeUnit unit, Runnable run) {
        return schedule(new Object(), time, unit, (scheduler, state) -> { run.run(); return Registration.EMPTY; });
    }
    /**
     * Schedule a task to run periodically after the given initial delay.
     * @param initialDelay the initial delay
     * @param period the periodicity of the task
     * @param unit the time unit of the delay time
     * @param run the task to run
     * @return the registration to cancel the execution of the task
     */
    default Registration schedule(long initialDelay, long period, TimeUnit unit, Runnable run) {
        return schedule(new Object(), initialDelay, period, unit, (scheduler, state) -> { run.run(); return Registration.EMPTY; });
    }
}
