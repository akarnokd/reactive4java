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

package java.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * Simple GWT version of the semaphore.
 * <p>Note that this might not work properly since
 * can't block in JavaScript.</p>
 * @author karnok, 2013.01.17.
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -5448444134808L;
    protected int permits;
    protected boolean fair;
    public Semaphore(int permits) {
    	this.permits = permits;
    }
    public Semaphore(int permits, boolean fair) {
    	this.permits = permits;
    	this.fair = fair;
    }
    public void acquire() throws InterruptedException {
    	permits++;
    }
    public void acquireUninterruptibly() {
    	permits++;
    }
    public boolean tryAcquire() {
    	permits++;
    	return true;
    }
    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
    	permits++;
    	return true;
	}
    public void release() {
    	permits--;
    }
    public void acquire(int permits) throws InterruptedException {
    	this.permits += permits;
    }
    public void acquireUninterruptibly(int permits) {
    	this.permits += permits;
    }
    public boolean tryAcquire(int permits) {
    	this.permits += permits;
    	return true;
    }
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
    	this.permits += permits;
    	return true;
	}
    public void release(int permits) {
    	this.permits -= permits;
    }
    public int availablePermits() {
    	return permits;
    }
    public int drainPermits() {
    	int p = permits;
    	permits = 0;
    	return p;
    }
    protected void reducePermits(int reduction) {
    	this.permits -= reduction;
    }
    public boolean isFair() {
    	return fair;
    }
    public final boolean hasQueuedThreads() {
    	return false;
    }
    public final int getQueueLength() {
    	return 0;
    }

}