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
 * A default base implementation of the observer interface which
 * ensures the proper event semantics and lets the observer
 * "close" itself.
 * <p>The default observer is an abstract class where the behavior
 * needs to be added through overriding the onNext, onError and
 * onFinish methods. These methods are
 * executed under a lock and according to the default event
 * semantics. If the observer wishes to finish itself, call
 * the done() method.<p>
 * @author akarnokd, 2013.11.09.
 */
public abstract class DefaultObserver<T> implements Observer<T> {
    private final Lock lock;
    private boolean done;
    public DefaultObserver() {
        lock = new ReentrantLock();
    }
    public DefaultObserver(Lock sharedLock) {
        lock = Objects.requireNonNull(sharedLock);
    }
    protected final void sync(Runnable run) {
        lock.lock();
        try {
            try {
                if (!done) {
                    run.run();
                }
            } catch (Throwable t) {
                done();
                onError(t);
            }
        } finally {
            lock.unlock();
        }
    }
    protected void done() {
        this.done = true;
    }
    protected boolean isDone() {
        return done;
    }
    @Override
    public final void next(T value) {
        sync(() -> { onNext(value); });
    }

    @Override
    public final void error(Throwable t) {
        sync(() -> { 
            done();
            onError(t); 
        });
    }
    @Override
    public final void finish() {
        sync(() -> { 
            done();
            onFinish(); 
        });
    }
    protected abstract void onNext(T value);
    protected abstract void onError(Throwable t);
    protected abstract void onFinish();
}
