/*
 * Copyright 2011-2012 David Karnok
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

package hu.akarnokd.reactive4java.reactive;

import javax.annotation.Nonnull;

/**
 * The observer who receives the notifications of T.
 * @author akarnokd
 * @param <T> the type of the notification values.
 */
public interface Observer<T> {
	/** 
	 * The next value is received. 
	 * @param value the next value 
	 */
	void next(T value);
	/** 
	 * An exception is received.
	 * @param ex the exception 
	 */
	void error(@Nonnull Throwable ex);
	/** No more values to expect. */
	void finish();
}
