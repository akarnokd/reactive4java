/*
 * Copyright 2011 David Karnok
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

package java.lang;

/**
 * The GWT thread concept, basically a current thread executor.
 * @author karnok, 2011.02.20.
 */
public class Thread implements Runnable {
	/** Is the thread interrupted? */
	boolean interruptedFlag;
	@Override
	public void run() {

	}
	/** @return is the current thread interrupted? */
	public boolean isInterrupted() {
		return interruptedFlag;
	}
	/** Interrupt the thread. */
	public void interrupt() {
		interruptedFlag = true;
	}
	/** The constant thread. */
	protected static final Thread CURRENT_THREAD = new Thread();
	/** @return the current thread object. */
	public static Thread currentThread() {
		return CURRENT_THREAD;
	}
//	/**
//	 * Start the thread.
//	 */
//	public void start() {
//		run();
//	}
}
