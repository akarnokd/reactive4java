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

package hu.akarnokd.reactive4java.base;

import javax.annotation.Nonnull;

/**
 * The observer which receives the notifications of primitive long.
 * @author akarnokd
 * @since 0.97
 */
public interface LongObserver {
	/** 
	 * The next value is received. 
	 * @param value the next value 
	 */
	void next(long value);
	/** 
	 * An exception is received.
	 * @param ex the exception 
	 */
	void error(@Nonnull Throwable ex);
	/** 
	 * No more values to expect. 
	 */
	void finish();
}