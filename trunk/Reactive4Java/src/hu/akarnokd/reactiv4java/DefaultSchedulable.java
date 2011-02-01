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

package hu.akarnokd.reactiv4java;

/**
 * The runnable instance which is aware of its scheduler's registration.
 * FIXME concurrency questions with the storage of the current future
 * @author akarnokd, 2011.01.29.
 */
public abstract class DefaultSchedulable extends ScheduledObserver<Void> {
	@Override
	public final void error(Throwable ex) {
		// not used
	}
	@Override
	public final void finish() {
		// not used
	}
	@Override
	public final void next(Void value) {
		// not used
	}
}
