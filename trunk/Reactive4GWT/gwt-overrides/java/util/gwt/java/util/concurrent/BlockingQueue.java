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

import java.util.Collection;
import java.util.Queue;

/**
 * The GWT representation of a blocking Queue for
 * Reactive4Java. Minimalist implementation
 * @author akarnokd, 2011.02.19.
 * @param <T> the queue element type
 */
public interface BlockingQueue<T> extends Queue<T> {
	/**
	 * Drain the contents into the target collection.
	 * @param target the target collection
	 */
	void drainTo(Collection<? super T> target);
	/** 
	 * @return take the head of the queue and block until it becomes available.
	 * @throws InterruptedException when the wait is interrupted 
	 */
	T take() throws InterruptedException;
}
