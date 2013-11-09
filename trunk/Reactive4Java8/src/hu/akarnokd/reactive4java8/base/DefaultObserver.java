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

import java.util.concurrent.locks.Lock;

/**
 * A default base implementation of the observer interface which
 * ensures the proper event semantics and lets the observer
 * "close" itself.
 * <p>You may composte an observable from lambda expressions with
 * the static methods of {@link Observer} such as create or createSafe.</p>
 * <p>The default observer is an abstract class where the behavior
 * needs to be added through overriding the onNext, onError and
 * onFinish methods. These methods are
 * executed under a lock and according to the default event
 * semantics. If the observer wishes to finish itself, call
 * the done() method.<p>
 * @author akarnokd, 2013.11.09.
 */
public abstract class DefaultObserver<T> implements Observer<T> {
    /** The lock sync object. */
    private final LockSync ls;
    /** Is the observer terminated? */
    private boolean done;
    /**
     * Constructor, creates a default lock sync object with non-fair
     * reentrant lock.
     */
    public DefaultObserver() {
        ls = new LockSync();
    }
    /**
     * Constructor, creates a lock sync object with the supplied
     * shared lock.
     * @param sharedLock the shared lock to use
     */
    public DefaultObserver(Lock sharedLock) {
        ls = new LockSync(sharedLock);
    }
    protected void done() {
        this.done = true;
    }
    protected boolean isDone() {
        return done;
    }
    @Override
    public final void next(T value) {
        ls.sync(() -> { 
            if (!done) {
                try {
                    onNext(value); 
                } catch (Throwable t) {
                    done = true;
                    onError(t);
                }
            }
        });
    }

    @Override
    public final void error(Throwable t) {
        ls.sync(() -> { 
            if (!done) {
                done = true;
                onError(t); 
            }
        });
    }
    @Override
    public final void finish() {
        ls.sync(() -> { 
            if (!done) {
                done = true;
                onFinish(); 
            }
        });
    }
    /**
     * Receives a value of type T.
     * @param value the value
     */
    protected abstract void onNext(T value);
    /**
     * Receives an exception.
     * @param t the exception
     */
    protected abstract void onError(Throwable t);
    /**
     * Receives a completion notification.
     */
    protected abstract void onFinish();
}
