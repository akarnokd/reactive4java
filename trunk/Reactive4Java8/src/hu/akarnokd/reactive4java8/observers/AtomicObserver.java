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

package hu.akarnokd.reactive4java8.observers;

import hu.akarnokd.reactive4java8.Observer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lock-free observer implementation.
 * @author akarnokd, 2013.11.17.
 */
public abstract class AtomicObserver<T> implements Observer<T> {
    /** Atomic state. */
    private static final int STATE_IDLE = 0;
    /** Atomic state. */
    private static final int STATE_NEXT = 1;
    /** Atomic state. */
    private static final int STATE_ERROR = 2;
    /** Atomic state. */
    private static final int STATE_FINISH = 3;
    /** The state variable. */
    private final AtomicInteger state = new AtomicInteger();

    @Override
    public final void next(T value) {
        do {
            int s = state.get();
            if (s == STATE_ERROR || s == STATE_FINISH) {
                return;
            }
        } while (!state.compareAndSet(STATE_IDLE, STATE_NEXT));
        try {
            onNext(value);
            state.compareAndSet(STATE_NEXT, STATE_IDLE);
        } catch (Throwable t) {
            state.set(STATE_ERROR);
            try {
                onError(t);
            } finally {
                close();
            }
        }
    }
    @Override
    public final void finish() {
        do {
            int s = state.get();
            if (s == STATE_ERROR || s == STATE_FINISH) {
                return;
            }
        } while (!state.compareAndSet(STATE_IDLE, STATE_FINISH));
        try {
            onFinish();
        } finally {
            close();
        }
    }
    @Override
    public final void error(Throwable t) {
        do {
            int s = state.get();
            if (s == STATE_ERROR || s == STATE_FINISH) {
                return;
            }
        } while (!state.compareAndSet(STATE_IDLE, STATE_ERROR));
        try {
            onError(t);
        } finally {
            close();
        }
    }
    /**
     * Sets the state to error if not already in finish state.
     */
    protected void setError() {
        int s;
        do {
            s = state.get();
            if (s == STATE_FINISH) {
                return;
            }
        } while (!state.compareAndSet(s, STATE_ERROR));
        close();
    }
    /**
     * Sets the state to finished state if not already in error state.
     * <p>Use this method to terminate the observer from the onNext()
     * as calling finish() won't work.</p>
     */
    protected void setFinish() {
        int s;
        do {
            s = state.get();
            if (s == STATE_ERROR) {
                return;
            }
        } while (!state.compareAndSet(s, STATE_FINISH));
        close();
    }
    /** 
     * Called once the observer moves to error or finish state. 
     * <p>Override to close the upstream registration for example.</p>
     */
    protected void close() { }
    /**
     * Handles a next() event.
     * @param value 
     */
    protected abstract void onNext(T value);
    /**
     * Handles an error() event.
     * @param t 
     */
    protected abstract void onError(Throwable t);
    /**
     * Handles a finish() event.
     */
    protected abstract void onFinish();

    @Override
    public Observer<T> toThreadSafe() {
        return this;
    }
    
    
}
