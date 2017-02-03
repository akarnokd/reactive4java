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

package hu.akarnokd.reactive4java.util;


import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class which ensures that only a single action is invoked.
 * It can be used in cases when multiple observers want to
 * do the same thing (e.g., fire an error() or finish())
 * but only one of them should ever succeed
 * @author akarnokd, 2011.01.31.
 */
public final class RunOnce {
	/** Marker that this instance is allowed to execute only one Action. */
	final AtomicBoolean once = new AtomicBoolean();
	/**
	 * Invoke the given action only if this RunOnce has not invoked
	 * anything before. The method ensures that if this RunOnce
	 * is invoked from multiple threads or multiple cases, only the
	 * very first one executes its submitted action.
	 * @param action the action to invoke
	 * @return returns true if the action was invoked
	 */
	public boolean invoke(final Action0 action) {
		if (once.compareAndSet(false, true)) {
			action.invoke();
			return true;
		}
		return false;
	}
	/**
	 * Invoke the given action only if this RunOnce has not invoked
	 * anything before. The method ensures that if this RunOnce
	 * is invoked from multiple threads or multiple cases, only the
	 * very first one executes its submitted action.
	 * @param <T> the parameter type
	 * @param action the action to invoke
	 * @param parameter the parameter to use when invoking the action.
	 * @return true if the action was invoked
	 */
	public <T> boolean invoke(final Action1<? super T> action, final T parameter) {
		if (once.compareAndSet(false, true)) {
			action.invoke(parameter);
		}
		return false;
	}
}
