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

package hu.akarnokd.reactive4java8.util;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Maintains a read-write lock and provides read and write synchronization
 * activities on them.
 * @author akarnokd, 2013.11.16.
 */
public class ReadWriteSync {
    /** The backing lock. */
    private final ReadWriteLock rwLock;
    /** The reader lock sync. */
    private final LockSync reader;
    /** The writer lock sync. */
    private final LockSync writer;
    /**
     * Default constructor, creates a non-fair reentrant read-write lock.
     */
    public ReadWriteSync() {
        this(new ReentrantReadWriteLock());
    }
    /**
     * Constructor, takes the shared read-write lock.
     * @param sharedRwLock 
     */
    public ReadWriteSync(ReadWriteLock sharedRwLock) {
        this.rwLock = Objects.requireNonNull(sharedRwLock);
        reader = new LockSync(this.rwLock.readLock());
        writer = new LockSync(this.rwLock.writeLock());
    }
    /**
     * Returns the reader's {@link LockSync} object.
     * @return 
     */
    public LockSync reader() {
        return reader;
    }
    /**
     * Returns the writer's {@link LockSync} object.
     * @return 
     */
    public LockSync writer() {
        return writer;
    }
    /**
     * Runs the activity while holding the read lock.
     * @param activity 
     */
    public void read(Runnable activity) {
        reader.sync(activity);
    }
    /**
     * Runs the activity while holding the write lock.
     * @param activity 
     */
    public void write(Runnable activity) {
        writer.sync(activity);
    }
    public <T> T read(Supplier<? extends T> supplier) {
        return reader.sync(supplier);
    }
    public boolean read(BooleanSupplier supplier) {
        return reader.sync(supplier);
    }
    public int read(IntSupplier supplier) {
        return reader.sync(supplier);
    }
    public long read(LongSupplier supplier) {
        return reader.sync(supplier);
    }
    public double read(DoubleSupplier supplier) {
        return reader.sync(supplier);
    }
    public <T> T write(Supplier<? extends T> supplier) {
        return writer.sync(supplier);
    }
    public boolean write(BooleanSupplier supplier) {
        return writer.sync(supplier);
    }
    public int write(IntSupplier supplier) {
        return writer.sync(supplier);
    }
    public long write(LongSupplier supplier) {
        return writer.sync(supplier);
    }
    public double write(DoubleSupplier supplier) {
        return writer.sync(supplier);
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades
     * to read-lock and performs the reading action.
     * @param writing
     * @param reading 
     */
    public void writeThenRead(Runnable writing, Runnable reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            reading.run();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades to read-lock and returns the value of the {@code reading} supplier.
     * @param <T>
     * @param writing
     * @param reading
     * @return 
     */
    public <T> T writeThenRead(Runnable writing, Supplier<? extends T> reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.get();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades to read-lock and returns the value of the {@code reading} supplier.
     * @param writing
     * @param reading
     * @return 
     */
    public boolean writeThenRead(Runnable writing, BooleanSupplier reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.getAsBoolean();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades to read-lock and returns the value of the {@code reading} supplier.
     * @param writing
     * @param reading
     * @return 
     */
    public int writeThenRead(Runnable writing, IntSupplier reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.getAsInt();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades to read-lock and returns the value of the {@code reading} supplier.
     * @param writing
     * @param reading
     * @return 
     */
    public long writeThenRead(Runnable writing, LongSupplier reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.getAsLong();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Executes the {@code writing} action under write-lock, downgrades to read-lock and returns the value of the {@code reading} supplier.
     * @param writing
     * @param reading
     * @return 
     */
    public double writeThenRead(Runnable writing, DoubleSupplier reading) {
        rwLock.writeLock().lock();
        try {
            writing.run();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.getAsDouble();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Gets a value from the {@code writing} supplier while holding the
     * write-lock, downgrades to read-lock and gives the intermediate
     * value to the {@code reading} consumer.
     * @param <T>
     * @param writing
     * @param reading 
     */
    public <T> void writeThenRead(Supplier<? extends T> writing, Consumer<? super T> reading) {
        T value;
        rwLock.writeLock().lock();
        try {
            value = writing.get();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            reading.accept(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Gets a value from the {@code writing} supplier while holding the
     * write-lock, downgrades to read-lock and gives the intermediate
     * value to the {@code reading} consumer.
     * @param <T>
     * @param writing
     * @param reading 
     */
    public <T> void writeThenRead(IntSupplier writing, IntConsumer reading) {
        int value;
        rwLock.writeLock().lock();
        try {
            value = writing.getAsInt();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            reading.accept(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Gets a value from the {@code writing} supplier while holding the
     * write-lock, downgrades to read-lock and gives the intermediate
     * value to the {@code reading} consumer.
     * @param <T>
     * @param writing
     * @param reading 
     */
    public <T> void writeThenRead(LongSupplier writing, LongConsumer reading) {
        long value;
        rwLock.writeLock().lock();
        try {
            value = writing.getAsLong();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            reading.accept(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Gets a value from the {@code writing} supplier while holding the
     * write-lock, downgrades to read-lock and gives the intermediate
     * value to the {@code reading} consumer.
     * @param <T>
     * @param writing
     * @param reading 
     */
    public <T> void writeThenRead(DoubleSupplier writing, DoubleConsumer reading) {
        double value;
        rwLock.writeLock().lock();
        try {
            value = writing.getAsDouble();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            reading.accept(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Gets a value from the {@code writing} supplier while holding the
     * write-lock, downgrades to read-lock and gives the intermediate
     * value to the {@code reading} function, which then 
     * transforms it into another value.
     * @param <T>
     * @param <U>
     * @param writing
     * @param reading
     * @return 
     */
    public <T, U> U writeThenRead(
        Supplier<? extends T> writing, 
        Function<? super T, ? extends U> reading) {
        T value;
        rwLock.writeLock().lock();
        try {
            value = writing.get();
            rwLock.readLock().lock();
        } finally {
            rwLock.writeLock().unlock();
        }
        try {
            return reading.apply(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
