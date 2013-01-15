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
package hu.akarnokd.reactive4java.reactive;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.CompositeCloseable;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Returns an observable which correlates two streams of values based on
 * their time when they overlapped and groups the results.
 * FIXME not sure how to implement it
 * @param <Left> the element type of the left stream
 * @param <Right> the element type of the right stream
 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
 * @param <Result> the type of the grouping based on the coincidence.
 * @return the new observable
 * @see #join(Observable, Observable, Func1, Func1, Func2)
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public class GroupJoin<Result, Left, Right, LeftDuration, RightDuration>
		implements Observable<Result> {
	/** */
	protected final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector;
	/** */
	protected final Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector;
	/** */
	protected final Observable<? extends Left> left;
	/** */
	protected final Observable<? extends Right> right;
	/** */
	protected final Func2<? super Left, ? super Observable<? extends Right>, ? extends Result> resultSelector;

	/**
	 * Constructor.
	 * @param left the left source of elements
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 */
	public GroupJoin(
			Observable<? extends Left> left,
			Observable<? extends Right> right,
			Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector,
			Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			Func2<? super Left, ? super Observable<? extends Right>, ? extends Result> resultSelector) {
		this.rightDurationSelector = rightDurationSelector;
		this.leftDurationSelector = leftDurationSelector;
		this.left = left;
		this.right = right;
		this.resultSelector = resultSelector;
	}

	@Override
	@Nonnull 
	public Closeable register(@Nonnull final Observer<? super Result> observer) {
		final CompositeCloseable composite = new CompositeCloseable();
		
		// FIXME implement
		
		return composite;
	}
}
