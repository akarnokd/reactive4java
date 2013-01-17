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
package java.util.concurrent.locks;

/**
 * The reentrant read-write lock implementation for GWT and
 * Reactive4Java. Basically, it is a no-op in GWT.
 * @author akarnokd, 2011.02.19.
 */
public class ReentrantReadWriteLock implements ReadWriteLock {
	/** The lock. */
	private Lock lock;
	/** Creates a new non-fair lock. */
	public ReentrantReadWriteLock() {
		lock = new ReentrantLock();
	}
	/**
	 * Creates a new lock based on the fairness indicator.
	 * @param fair the fairness indicator
	 */
	public ReentrantReadWriteLock(boolean fair) {
		lock = new ReentrantLock(fair);
	}
	@Override
	public Lock readLock() {
		return lock;
	}

	@Override
	public Lock writeLock() {
		return lock;
	}

}
