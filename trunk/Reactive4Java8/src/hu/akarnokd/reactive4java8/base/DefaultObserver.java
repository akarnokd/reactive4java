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

/**
 * A default base implementation of the observer interface which
 * ensures the proper event semantics and lets the observer
 * "close" itself.
 * <p>You may composte an observable from lambda expressions with
 * the static methods of {@link Observer} such as create or createSafe.</p>
 * <p>An overloaded constructor gives the opportunity to close a
 * registration in case an error or finish event occurs.</p>
 * <p>The default observer is an abstract class where the behavior
 * needs to be added through overriding the onNext, onError and
 * onFinish methods. These methods are
 * executed under a lock and according to the default event
 * semantics. If the observer wishes to finish itself, call
 * the done() method.<p>
 * @author akarnokd, 2013.11.09.
 */
public abstract class DefaultObserver<T> implements Observer<T> {
    protected final LockSync ls;
    private boolean done;
    protected final Registration reg;
    /**
     * Default observer with empty registration and non-fair reentrant lock.
     */
    public DefaultObserver() {
        this(Registration.EMPTY);
    }
    public DefaultObserver(Lock lock) {
        this(Registration.EMPTY, lock);
    }
    public DefaultObserver(Registration reg) {
        this.ls = new LockSync();
        this.reg = Objects.requireNonNull(reg);
    }
    public DefaultObserver(Registration reg, Lock lock) {
        this.ls = new LockSync(lock);
        this.reg = Objects.requireNonNull(reg);
    }
    /** 
     * Set the done flag to true, preventing further delivery
     * of events.
     */
    protected void done() {
        this.done = true;
    }
    /**
     * Returns the done flag value.
     * @return 
     */
    protected boolean isDone() {
        return done;
    }
    @Override
    public final void next(T value) {
        boolean c = ls.sync(() -> {
            if (!done) {
                try {
                    onNext(value);
                } catch (Throwable t) {
                    done = true;
                    onError(t);
                }
            }
            return done;
        });
        if (c) {
            reg.close();
        }
    }
    @Override
    public final void error(Throwable t) {
        boolean c = ls.sync(() -> {
            if (!done) {
                done = true;
                onError(t);
            }
            return done;
        });
        if (c) {
            reg.close();
        }
    }
    @Override
    public final void finish() {
        boolean c = ls.sync(() -> {
            if (!done) {
                done = true;
                onFinish();
            }
            return done;
        });
        if (c) {
            reg.close();
        }
    }
    /** Closes the associated registration. */
    protected void close() {
        reg.close();
    }
    /**
     * Called when a value arrives.
     * @param value 
     */
    protected abstract void onNext(T value);
    /**
     * Called when an exception arrives.
     * @param t 
     */
    protected abstract void onError(Throwable t);
    /**
     * Called when a finish event arrives.
     */
    protected abstract void onFinish();
}
