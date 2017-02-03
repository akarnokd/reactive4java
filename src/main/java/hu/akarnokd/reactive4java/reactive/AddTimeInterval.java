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
package hu.akarnokd.reactive4java.reactive;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.TimeInterval;
import hu.akarnokd.reactive4java.util.Schedulers;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Returns an observable which provides a TimeInterval of Ts which
 * records the elapsed time between successive elements.
 * The time interval is evaluated using the System.nanoTime() differences
 * as nanoseconds.
 * The first element contains the time elapsed since the registration occurred.
 * @author akarnokd, 2013.01.13.
 * @param <T> the time source
 * @since 0.97
 */
public final class AddTimeInterval<T> implements
        Observable<TimeInterval<T>> {
    /** The source sequence. */
    private final Observable<? extends T> source;

    /**
     * Constructor.
     * @param source the source sequence
     */
    public AddTimeInterval(@Nonnull Observable<? extends T> source) {
        this.source = source;
    }

    @Override
    @Nonnull 
    public Closeable register(@Nonnull final Observer<? super TimeInterval<T>> observer) {
        return source.register(new Observer<T>() {
            long lastTime = Schedulers.now();
            @Override
            public void error(@Nonnull Throwable ex) {
                observer.error(ex);
            }

            @Override
            public void finish() {
                observer.finish();
            }

            @Override
            public void next(T value) {
                long t2 = Schedulers.now();
                observer.next(TimeInterval.of(value, t2 - lastTime));
                lastTime = t2;
            }

        });
    }
}
