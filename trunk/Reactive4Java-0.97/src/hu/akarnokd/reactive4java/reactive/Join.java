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

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.Observers;
import hu.akarnokd.reactive4java.util.Producer;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.SingleCloseable;
import hu.akarnokd.reactive4java.util.Sink;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Correlates elements of two sequences based on overlapping durations.
 * <p>Windows are terminated by either a next() or finish call from the duration
 * observables.</p>
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 * @param <Left> the element type of the left stream
 * @param <Right> the element type of the right stream
 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
 * @param <Result> the type of the grouping based on the coincidence.
 */
public class Join<Left, Right, LeftDuration, RightDuration, Result> extends
		Producer<Result> {
	/** */
	protected Observable<? extends Left> left;
	/** */
	protected Observable<? extends Right> right;
	/** */
	protected Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector;
	/** */
	protected Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector;
	/** */
	protected Func2<? super Left, ? super Right, ? extends Result> resultSelector;

	/**
	 * Constructor.
	 * @param left the left source of elements
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 */
	public Join(
			final Observable<? extends Left> left,
			final Observable<? extends Right> right,
			final Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector,
			final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			final Func2<? super Left, ? super Right, ? extends Result> resultSelector
	) {
		this.left = left;
		this.right = right;
		this.leftDurationSelector = leftDurationSelector;
		this.rightDurationSelector = rightDurationSelector;
		this.resultSelector = resultSelector;
	}

	@Override
	protected Closeable run(Observer<? super Result> observer,
			Closeable cancel, Action1<Closeable> setSink) {
		ResultSink sink = new ResultSink(observer, cancel);
		setSink.invoke(sink);
		return sink.run();
	}
	/** The result sink. */
	class ResultSink extends Sink<Result> {
		/** The lock protecting the structures. */
		protected final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
		/** The composite closeable. */
		protected final CompositeCloseable group = new CompositeCloseable();
		/** The left source completed. */
		@GuardedBy("lock")
		protected boolean leftDone;
		/** The left window id. */
		@GuardedBy("lock")
		protected int leftId;
		/** The left value map per window. */
		@GuardedBy("lock")
		protected final Map<Integer, Left> leftMap = new HashMap<Integer, Left>();
		/** The right source completed. */
		@GuardedBy("lock")
		protected boolean rightDone;
		/** The right window id. */
		@GuardedBy("lock")
		protected int rightId;
		/** The right value map per window. */
		@GuardedBy("lock")
		protected final Map<Integer, Right> rightMap = new HashMap<Integer, Right>();
		/**
		 * Constructor.
		 * @param observer the original observer
		 * @param cancel the cancel token
		 */
		public ResultSink(Observer<? super Result> observer, Closeable cancel) {
			super(observer, cancel);
		}
		/**
		 * @return the sink body to run
		 */
		public Closeable run() {
			
			SingleCloseable leftRegistration = new SingleCloseable();
			SingleCloseable rightRegistration = new SingleCloseable();
			
			group.add(leftRegistration, rightRegistration);
			
			leftRegistration.set(Observers.registerSafe(left, new LeftObserver(leftRegistration)));
			rightRegistration.set(Observers.registerSafe(right, new RightObserver(rightRegistration)));
			
			return group;
		}
		/** The left sequence observer. */
		class LeftObserver implements Observer<Left> {
			/** Close the observer. */
			protected final Closeable self;
			/**
			 * Constructor.
			 * @param self the self cancel handler
			 */
			public LeftObserver(Closeable self) {
				this.self = self;
			}
			/**
			 * Terminate a window.
			 * @param id the window id
			 * @param resource the resource
			 */
			protected void expire(int id, Closeable resource) {
				lock.lock();
				try {
					if (leftMap.remove(id) != null && leftMap.isEmpty() && leftDone) {
						observer.get().finish();
						ResultSink.this.closeSilently();
					}
				} finally {
					lock.unlock();
				}
				group.removeSilently(resource);
			}
			@Override
			public void next(Left value) {
				// TODO Auto-generated method stub
				
				int id = 0;
				lock.lock();
				try {
					id = leftId++;
					leftMap.put(id, value);
				} finally {
					lock.unlock();
				}
				
				SingleCloseable md = new SingleCloseable();
				group.add(md);
				
				Observable<LeftDuration> duration = null;
				Observer<? super Result> o = observer.get();
				try {
					duration = leftDurationSelector.invoke(value);
				} catch (Throwable t) {
					o.error(t);
					ResultSink.this.closeSilently();
					return;
				}
				
				md.set(Observers.registerSafe(duration, new LeftDurationObserver(id, md)));
				
				lock.lock();
				try {
					for (Right r : rightMap.values()) {
						Result result = null;
						try {
							result = resultSelector.invoke(value, r);
						} catch (Throwable t) {
							o.error(t);
							ResultSink.this.closeSilently();
							return;
						}
						o.next(result);
					}
				} finally {
					lock.unlock();
				}
			}
			/** The left duration observer. */
			class LeftDurationObserver implements Observer<LeftDuration> {
				/** The window id. */
				protected final int id;
				/** The close handler. */
				protected final Closeable handle;
				/**
				 * Constructor.
				 * @param id window id
				 * @param handle the close handler
				 */
				public LeftDurationObserver(int id, Closeable handle) {
					this.id = id;
					this.handle = handle;
				}
				@Override
				public void error(@Nonnull Throwable ex) {
					LeftObserver.this.error(ex);
				}
				@Override
				public void finish() {
					expire(id, handle);
				}
				@Override
				public void next(LeftDuration value) {
					expire(id, handle);
				}
			}
			
			@Override
			public void error(@Nonnull Throwable ex) {
				lock.lock();
				try {
					observer.get().error(ex);
					ResultSink.this.closeSilently();
				} finally {
					lock.unlock();
				}
			}
			@Override
			public void finish() {
				lock.lock();
				try {
					leftDone = true;
					if (rightDone || leftMap.isEmpty()) {
						observer.get().finish();
						ResultSink.this.closeSilently();
					} else {
						Closeables.closeSilently(self);
					}
				} finally {
					lock.unlock();
				}
			}
		}
		/** The right sequence observer. */
		class RightObserver implements Observer<Right> {
			/** Close the observer. */
			protected final Closeable self;
			/**
			 * Constructor.
			 * @param self the self cancel handler
			 */
			public RightObserver(Closeable self) {
				this.self = self;
			}
			/** 
			 * Close a window by the given id.
			 * @param id the window id
			 * @param resource the resource to close 
			 */
			protected void expire(int id, Closeable resource) {
				lock.lock();
				try {
					if (rightMap.remove(id) != null && rightMap.isEmpty() && rightDone) {
						observer.get().finish();
						ResultSink.this.closeSilently();
					}
				} finally {
					lock.unlock();
				}
				group.removeSilently(resource);
			}
			@Override
			public void next(Right value) {
				int id = 0;
				lock.lock();
				try {
					id = rightId++;
					rightMap.put(id, value);
				} finally {
					lock.unlock();
				}
				
				SingleCloseable md = new SingleCloseable();
				group.add(md);
				
				Observable<RightDuration> duration = null;
				Observer<? super Result> o = observer.get();
				try {
					duration = rightDurationSelector.invoke(value);
				} catch (Throwable t) {
					o.error(t);
					ResultSink.this.closeSilently();
					return;
				}
				
				md.set(Observers.registerSafe(duration, new RightDurationObserver(id, md)));
				
				lock.lock();
				try {
					for (Left lv : leftMap.values()) {
						Result result = null;
						try {
							result = resultSelector.invoke(lv, value);
						} catch (Throwable t) {
							o.error(t);
							ResultSink.this.closeSilently();
							return;
						}
						o.next(result);
					}
				} finally {
					lock.unlock();
				}
			}
			/** The right duration observer. */
			class RightDurationObserver implements Observer<RightDuration> {
				/** The window id. */
				protected final int id;
				/** The close handle. */
				protected final Closeable handle;
				/**
				 * Constructor.
				 * @param id the window handle
				 * @param handle the associated resource close
				 */
				public RightDurationObserver(int id, Closeable handle) {
					this.id = id;
					this.handle = handle;
				}
				@Override
				public void error(@Nonnull Throwable ex) {
					RightObserver.this.error(ex);
				}
				@Override
				public void finish() {
					expire(id, handle);
				}
				@Override
				public void next(RightDuration value) {
					expire(id, handle);
				}
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				lock.lock();
				try {
					observer.get().error(ex);
					ResultSink.this.closeSilently();
				} finally {
					lock.unlock();
				}
			}
			@Override
			public void finish() {
				lock.lock();
				try {
					rightDone = true;
					if (leftDone || rightMap.isEmpty()) {
						observer.get().finish();
						ResultSink.this.closeSilently();
					} else {
						Closeables.closeSilently(self);
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}
}
