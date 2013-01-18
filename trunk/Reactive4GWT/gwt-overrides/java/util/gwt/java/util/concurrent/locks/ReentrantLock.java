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
 * The reentrant lock implementation for GWT and
 * Reactive4Java. Basically, it is a no-op in GWT.
 * @author akarnokd, 2011.02.19.
 */
public class ReentrantLock implements Lock {
	/** Creates a new non-fair lock. */
	public ReentrantLock() {
		
	}
	/**
	 * Creates a new lock based on the fairness indicator.
	 * @param fair the fairness indicator
	 */
	public ReentrantLock(boolean fair) {
		
	}
	@Override
	public void lock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void unlock() {
		// TODO Auto-generated method stub

	}
	@Override
	public boolean tryLock() {
		return true;
	}
}
