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

package hu.akarnokd.reactive4java8;

import java.util.Objects;

/**
 * Base class for the materialized view of events.
 * @author karnok
 */
public final class Notification<T> {
    private final boolean hasValue;
    private final T value;
    private final Throwable exception;
    public Notification() {
        hasValue = false;
        value = null;
        exception = null;
    }
    public Notification(T value) {
        this.hasValue = true;
        this.value = value;
        this.exception = null;
    }
    public Notification(Throwable t) {
        hasValue = false;
        value = null;
        this.exception = Objects.requireNonNull(t);
    }
    public boolean hasValue() {
        return hasValue;
    }
    public boolean isEmpty() {
        return !hasValue && exception == null;
    }
    public boolean isException() {
        return exception != null;
    }
    public T value() {
        return value;
    }
    public Throwable exception() {
        return exception;
    }
    public void apply(Observer<? super T> observer) {
        if (exception != null) {
            observer.error(exception);
        } else
        if (hasValue) {
            try {
                observer.next(value);
            } catch (Throwable t) {
                observer.error(t);
            }
        } else {
            observer.finish();
        }
    }
}
