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
package java.util.concurrent.locks;

/**
 * A minimalist <code>java.util.concurrent.Lock</code> interface
 * to support the Reactive4Java's concurrency dependant operators.
 * Lock in GWT is basically a NO-OP construct.
 * @author akarnokd, 2011.02.19.
 */
public interface Lock {
	/** Activate the lock. */
	void lock();
	/** Deactivate the lock. */
	void unlock();
}
