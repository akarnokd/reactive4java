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

import hu.akarnokd.reactive4java8.registrations.SingleRegistration;
import hu.akarnokd.reactive4java8.Scheduler;
import hu.akarnokd.reactive4java8.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Ensures that the tasks submitted to the underlying scheduler
 * never overlap with each other (as if they would run on
 * a single threaded pool).
 * Call close() to stop all pending tasks.
 * @author akarnokd, 2013.11.09.
 */
public class SingleLaneScheduling implements Registration {
    /** The wrapped scheduler. */
    private final Scheduler scheduler;
    /** The queue of tasks.*/
    private final BlockingQueue<FutureTask<?>> queue = new LinkedBlockingQueue<>();
    /** The length of the queue. */
    private final AtomicInteger queueLength = new AtomicInteger();
    /** The lock to let a runTasks() finish before running the next runTasks(). */
    private final Lock taskLock = new ReentrantLock();
    /** The registration that tracks the scheduler runTasks().*/
    private final SingleRegistration scheduleReg = new SingleRegistration();
    /** Used to exclude any concurrent schedule() and close() calls. */
    private final Lock cancelLock = new ReentrantLock();
    /**
     * Constructor, takes a scheduler object to manage the
     * executions.
     * @param scheduler the scheduler
     */
    public SingleLaneScheduling(Scheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
    }
    /**
     * Schedule the given task.
     * @param task the task to schedule, long running tasks
     * should check for the Thread.currentThread().isInterrupted() flag
     * to detect if they were cancelled in the mean time.
     * @return the registration to cancel the task.
     */
    public Registration schedule(Runnable task) {
        cancelLock.lock();
        try {
            if (!scheduleReg.isClosed()) {
                FutureTask<?> ftask = new FutureTask<>(task, null);
                queue.add(ftask);
                if (queueLength.incrementAndGet() == 1) {
                    scheduleQueue();
                }
                return () -> {
                    ftask.cancel(true);
                };
            }
            return Registration.EMPTY;
        } finally {
            cancelLock.unlock();
        }
    }
    /**
     * Schedule the execution of the queued tasks.
     */
    private void scheduleQueue() {
        scheduleReg.set(scheduler.schedule(() -> {
            // exclude each runTasks as there is a window with queueLength
            // being decremented to zero and a new runtask
            // being run
            taskLock.lock();
            try {
                runTasks();
            } finally {
                taskLock.unlock();
            }
        }));
    }
    /**
     * Execute the queued tasks.
     */
    private void runTasks() {
        while (!Thread.currentThread().isInterrupted()) {
            FutureTask<?> t = queue.poll();
            if (t != null) {
                try {
                    t.run();
                } finally {
                    if (queueLength.decrementAndGet() <= 0) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    @Override
    public void close() {
        cancelLock.lock();
        try {
            scheduleReg.close();
        } finally {
            cancelLock.unlock();
        }
        // cancel remaining pending tasks
        List<FutureTask<?>> tasks = new ArrayList<>();
        queue.drainTo(tasks);
        queueLength.addAndGet(-tasks.size());
        tasks.forEach(t -> t.cancel(true));
    }
    
}
