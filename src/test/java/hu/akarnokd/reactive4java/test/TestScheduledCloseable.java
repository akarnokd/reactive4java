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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.scheduler.DefaultScheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.ScheduledCloseable;

import java.io.IOException;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the behavoir of ScheduledCloseable.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class TestScheduledCloseable {
    /** 
     * Test simple closing. 
     * @throws IOException due to Closeable.close();
     * @throws InterruptedException due wait
     */
    @Test(timeout = 2000)
    public void simpleTest() throws IOException, InterruptedException {
        Scheduler scheduler = new DefaultScheduler(1);
        
        DebugCloseable mc = new DebugCloseable();
        
        ScheduledCloseable c = new ScheduledCloseable(scheduler, mc);
        
        Closeables.closeSilently(c);
        
        while (!mc.isClosed()) {
            Thread.sleep(100);
        }
        
        Assert.assertNotSame(mc, c.get());
    }

}
