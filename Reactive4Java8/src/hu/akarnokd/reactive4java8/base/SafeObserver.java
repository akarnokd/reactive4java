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

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Observer wrapper that ensures the
 * {@code next* (error|finish)?} event pattern on its wrapped observer.
 * @param <T> the value type
 * @author akarnokd, 2013.11.09
 */
public final class SafeObserver<T> implements Observer<T> {
    private final Observer<T> wrapped;
    private final Lock lock;
    private boolean done;

    /**
     * Constructor, wraps the observer.
     * @param observer the observer to wrap
     */
    public SafeObserver(Observer<T> observer) {
        this.wrapped = Objects.requireNonNull(observer);
        lock = new ReentrantLock();
    }

    protected void sync(Runnable run) {
        lock.lock();
        try {
            run.run();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void next(T value) {
        sync(() -> {
            if (!done) {
                try {
                    wrapped.next(value);
                } catch (Throwable t) {
                    error(t);
                }
            }
        });
    }

    @Override
    public void error(Throwable t) {
        sync(() -> {
            if (!done) {
                done = true;
                wrapped.error(t);
            }
        });
    }

    @Override
    public void finish() {
        sync(() -> {
            if (!done) {
                done = true;
                wrapped.finish();
            }
        });
    }

}
