/*
 * Copyright 2011-2013 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Option;

import javax.annotation.Nonnull;

/**
 * Container that may contain a single value or an exception, but not both.
 * <p>The caller may add a single value or exception to this
 * container. Any subsequent add attempt results in IllegalStateException.
 * <p>The caller may retrieve the value or exception from this container,
 * which is then removed from the container; the caller may add 
 * a new value or exception. If the caller attempts to remove
 * a value or exception after a value, it will result in IllegalStateException.</p>
 * <p>The class is not thread safe.</p>
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 * @param <T> the contained value type
 */
public final class SingleOption<T> {
    /** Indicator for having a value or error. */
    protected boolean hasContent;
    /** The contained value. */
    protected T value;
    /** The stored exception. */
    protected Throwable error;
    /**
     * Add a new value to the container.
     * The container must be empty.
     * @param value the value to add
     */
    public void add(T value) {
        ensureEmpty();
        hasContent = true;
        this.value = value;
    }
    /**
     * Add a new error to the container.
     * The container must be empty.
     * @param ex the exception to add
     */
    public void addError(@Nonnull Throwable ex) {
        ensureEmpty();
        hasContent = true;
        this.error = ex;
    }
    /**
     * @return test if the container is empty
     */
    public boolean isEmpty() {
        return !hasContent;
    }
    /** Throws an IllegalStateException if the container is full. */
    protected void ensureEmpty() {
        if (hasContent) {
            throw new IllegalStateException("Full");
        }
    }
    /** Throws an IllegalStateException if the container is empty. */
    protected void ensureFull() {
        if (!hasContent) {
            throw new IllegalStateException("Empty");
        }
    }
    /**
     * Takes the current value or throws a RuntimeException
     * if there is an error instead.
     * <p>The container must be full, and gets empty after the call.</p>
     * @return the value contained
     */
    public T take() {
        ensureFull();
        hasContent = false;
        Throwable t = error;
        if (t != null) {
            error = null;
            Throwables.throwAsUnchecked(t);
        }
        T result = value;
        value = null;
        return result;
    }
    /**
     * Takes just the exception from this container.
     * The container becomes empty after the call.
     * If the container doesn't hold an exception, an
     * IllegalStateException is thrown.
     * @return the exception
     */
    @Nonnull
    public Throwable takeError() {
        ensureFull();
        Throwable t = error;
        if (t != null) {
            error = null;
            return t;
        }
        throw new IllegalStateException("no error");
    }
    /**
     * @return check if there is an error in this container
     */
    public boolean hasError() {
        return hasContent && error != null;
    }
    /**
     * @return Consumes the content of this container in a form of
     * an option instance. The container becomes empty after it.
     */
    @Nonnull
    public Option<T> option() {
        if (hasContent) {
            Throwable t = error;
            if (t != null) {
                error = null;
                return Option.error(t);
            }
            T v = value;
            value = null;
            return Option.some(v);
        }
        return Option.none();
    }
    /**
     * Add a new optional value to the container.
     * The container must be empty.
     * Throws IllegalArgumentException if o is None.
     * @param o the option to add
     */
    public void addOption(@Nonnull Option<? extends T> o) {
        if (o.hasValue()) {
            add(o.value());
        } else
        if (o.hasError()) {
            addError(Option.getError(o));
        } else {
            throw new IllegalArgumentException("Option.None has no meaning here");
        }
    }
}
