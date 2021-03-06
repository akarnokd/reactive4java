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
package hu.akarnokd.reactive4java.swing;

import hu.akarnokd.reactive4java.base.Scheduler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The default Event Dispatch Thread scheduler implementation.
 * Which ensures that all tasks submitted to it
 * will run on the EDT.
 * <p>Use with the <code>Reactive.subscribeOn</code> and <code>Reactive.observeOn</code>
 * operators to ensure your code interacts with Swing objects on the EDT.</p>
 * @author akarnokd, 2011.02.02.
 * @see hu.akarnokd.reactive4java.reactive.Reactive#observeOn(hu.akarnokd.reactive4java.base.Observable, Scheduler)
 * @see hu.akarnokd.reactive4java.reactive.Reactive#registerOn(hu.akarnokd.reactive4java.base.Observable, Scheduler)
 */
public class DefaultEdtScheduler implements Scheduler {
    /**
     * Helper class that has semantics for cancellation.
     * @author akarnokd, 2011.02.02.
     */
    static class EdtRunnable implements Runnable, Closeable {
        /** The wrapped runnable. */
        final Runnable run;
        /** Should the run() method still execute its body? */
        final AtomicBoolean alive = new AtomicBoolean(true);
        /**
         * Constructor.
         * @param run the runnable
         */
        public EdtRunnable(Runnable run) {
            if (run == null) {
                throw new IllegalArgumentException("run is null");
            }
            this.run = run;
        }
        @Override
        public void run() {
            if (alive.compareAndSet(true, false)) {
                run.run();
            }
        }
        @Override
        public void close() throws IOException {
            alive.set(false);
        }
    }
    @Override
    @Nonnull 
    public Closeable schedule(@Nonnull Runnable run) {
        EdtRunnable t = new EdtRunnable(run);
        SwingUtilities.invokeLater(t);
        return t;
    }

    @Override
    @Nonnull 
    public Closeable schedule(
            @Nonnull final Runnable run, 
            long delay, 
            @Nonnull final TimeUnit unit) {
        final Timer t = new Timer((int)unit.convert(delay, TimeUnit.MILLISECONDS), null);
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
                t.stop();
            }
        });
        t.start();
        return new Closeable() {
            @Override
            public void close() throws IOException {
                t.stop();
            }
        };
    }

    @Override
    @Nonnull 
    public Closeable schedule(
            @Nonnull final Runnable run, 
            long initialDelay, 
            long betweenDelay, 
            @Nonnull TimeUnit unit) {
        final Timer t = new Timer((int)unit.convert(initialDelay, TimeUnit.MILLISECONDS), null);
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        });
        t.setDelay((int)unit.convert(betweenDelay, TimeUnit.MILLISECONDS));
        t.start();
        return new Closeable() {
            @Override
            public void close() throws IOException {
                t.stop();
            }
        };
    }

}
