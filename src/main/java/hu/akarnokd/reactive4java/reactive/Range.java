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
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.util.DefaultRunnable;

import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.range operators.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public final class Range {
    /** Helper class. */
    private Range() { }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsLong implements Observable<Long> {
        /***/
        private final long start;
        /** */
        private final Scheduler pool;
        /** */
        private final long count;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param pool the scheduler pool where to emit the values.
         */
        public AsLong(long start, long count, Scheduler pool) {
            this.start = start;
            this.count = count;
            this.pool = pool;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super Long> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    for (long i = start; i < start + count && !cancelled(); i++) {
                        observer.next(i);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsInt implements Observable<Integer> {
        /** */
        private final int count;
        /** */
        private final Scheduler pool;
        /** */
        private final int start;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param pool the scheduler pool where to emit the values.
         */
        public AsInt(int start, int count, Scheduler pool) {
            this.count = count;
            this.pool = pool;
            this.start = start;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super Integer> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    for (int i = start; i < start + count && !cancelled(); i++) {
                        observer.next(i);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsFloat implements Observable<Float> {
        /** */
        private final Scheduler pool;
        /** */
        private final float step;
        /** */
        private final float start;
        /** */
        private final int count;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param step the delta between values
         * @param pool the scheduler pool where to emit the values.
         */
        public AsFloat(float start, int count, float step, Scheduler pool) {
            this.pool = pool;
            this.step = step;
            this.start = start;
            this.count = count;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super Float> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    for (int i = 0; i < count && !cancelled(); i++) {
                        observer.next(start + i * step);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsDouble implements Observable<Double> {
        /** */
        private final Scheduler pool;
        /** */
        private final double start;
        /** */
        private final int count;
        /** */
        private final double step;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param step the delta between values
         * @param pool the scheduler pool where to emit the values.
         */
        public AsDouble(double start, int count, double step, Scheduler pool) {
            this.pool = pool;
            this.start = start;
            this.count = count;
            this.step = step;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super Double> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    for (int i = 0; i < count && !cancelled(); i++) {
                        observer.next(start + i * step);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsBigInteger implements Observable<BigInteger> {
        /** */
        private final BigInteger start;
        /** */
        private final BigInteger count;
        /** */
        private final Scheduler pool;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param pool the scheduler pool where to emit the values.
         */
        public AsBigInteger(BigInteger start, BigInteger count, Scheduler pool) {
            this.start = start;
            this.count = count;
            this.pool = pool;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super BigInteger> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    BigInteger end = start.add(count);
                    for (BigInteger i = start; i.compareTo(end) < 0
                    && !cancelled(); i = i.add(BigInteger.ONE)) {
                        observer.next(i);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }
    /**
     * Generates a range of values.
     * @author akarnokd, 2013.01.16.
     */
    public static final class AsBigDecimal implements Observable<BigDecimal> {
        /** */
        private final int count;
        /** */
        private final Scheduler pool;
        /** */
        private final BigDecimal start;
        /** */
        private final BigDecimal step;

        /**
         * Constructor.
         * @param start the start value
         * @param count the number of values to emit
         * @param step the delta between values
         * @param pool the scheduler pool where to emit the values.
         */
        public AsBigDecimal(BigDecimal start, int count,
                BigDecimal step, Scheduler pool) {
            this.count = count;
            this.pool = pool;
            this.start = start;
            this.step = step;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super BigDecimal> observer) {
            DefaultRunnable s = new DefaultRunnable() {
                @Override
                public void onRun() {
                    BigDecimal value = start;
                    for (int i = 0; i < count && !cancelled(); i++) {
                        observer.next(value);
                        value = value.add(step);
                    }
                    if (!cancelled()) {
                        observer.finish();
                    }
                }
            };
            return pool.schedule(s);
        }
    }

}
