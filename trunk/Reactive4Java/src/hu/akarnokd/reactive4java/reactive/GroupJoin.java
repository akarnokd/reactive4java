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
import hu.akarnokd.reactive4java.base.Subject;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.Observers;
import hu.akarnokd.reactive4java.util.Producer;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.RefCountCloseable;
import hu.akarnokd.reactive4java.util.RefCountObservable;
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
 * Returns an observable which correlates two streams of values based on
 * their time when they overlapped and groups the results.
 * <p>Windows are terminated by either a next() or finish call from the duration
 * observables.</p>
 * @param <Left> the element type of the left stream
 * @param <Right> the element type of the right stream
 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
 * @param <Result> the type of the grouping based on the coincidence.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public class GroupJoin<Result, Left, Right, LeftDuration, RightDuration>
extends Producer<Result> {
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
	protected Closeable run(Observer<? super Result> observer,
			Closeable cancel, Action1<Closeable> setSink) {
		ResultSink sink = new ResultSink(observer, cancel);
		setSink.invoke(sink);
		return sink.run();
	}
	/** 
	 * The result sink.
	 * @author akarnokd, 2013.01.16.
	 */
	class ResultSink extends Sink<Result> {
		/** The global guard lock. */
		protected final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
		/** The group closeable. */
		protected final CompositeCloseable group = new CompositeCloseable();
		/** The reference counting closeable. */
		protected final RefCountCloseable refCount = new RefCountCloseable(group);
		/** The left open windows running identifier. */
		@GuardedBy("lock")
		protected int leftId;
		/** The right observer map. */
		@GuardedBy("lock")
		protected Map<Integer, Observer<Right>> leftMap = new HashMap<Integer, Observer<Right>>();
		/** The right open windows running identifier. */
		@GuardedBy("lock")
		protected int rightId;
		/** The right value map. */
		@GuardedBy("lock")
		protected Map<Integer, Right> rightMap = new HashMap<Integer, Right>();
		/**
		 * Constructor.
		 * @param observer the observer to handle
		 * @param cancel the cancel handler
		 */
		public ResultSink(Observer<? super Result> observer, Closeable cancel) {
			super(observer, cancel);
		}
		/**
		 * Performs the composite registration action.
		 * @return the closeable to terminate the whole stream
		 */
		public Closeable run() {
			SingleCloseable leftReg = new SingleCloseable();
			group.add(leftReg);
			
			SingleCloseable rightReg = new SingleCloseable();
			group.add(rightReg);
			
			leftReg.set(Observers.registerSafe(left, new LeftObserver(leftReg)));
			rightReg.set(Observers.registerSafe(right, new RightObserver(rightReg)));
			
			return refCount;
		}
		/** The left value observer. */
		class LeftObserver implements Observer<Left> {
			/** The self-closing handle. */
			protected final Closeable handle;
			/**
			 * Constructor.
			 * @param handle the self-closing handle
			 */
			public LeftObserver(Closeable handle) {
				this.handle = handle;
			}
			/**
			 * Terminate the processing of a window and release its resource.
			 * @param id the identifier
			 * @param gr the right group observer
			 * @param resource the resource to close
			 */
			protected void expire(int id, Observer<Right> gr, Closeable resource) {
				lock.lock();
				try {
					if (leftMap.remove(id) != null) {
						gr.finish();
					}
				} finally {
					lock.unlock();
				}
				group.removeSilently(resource);
			}
			@Override
			public void next(Left value) {
				Subject<Right, Right> s = new DefaultObservable<Right>();
				int id = 0;
				lock.lock();
				try {
					id = leftId++;
					leftMap.put(id, s);
				} finally {
					lock.unlock();
				}
				
				Observable<Right> window = new RefCountObservable<Right>(s, refCount);
				
				SingleCloseable md = new SingleCloseable();
				group.add(md);
				Observable<LeftDuration> duration = null;
				try {
					duration = leftDurationSelector.invoke(value);
				} catch (Throwable t) {
					error(t);
					return;
				}
				
				md.set(Observers.registerSafe(duration, new LeftDurationObserver(id, s, md)));
				
				Result result = null;
				try {
					result = resultSelector.invoke(value, window);
				} catch (Throwable t) {
					error(t);
					return;
				}
				
				lock.lock();
				try {
					ResultSink.this.observer.get().next(result);
					
					for (Right r : rightMap.values()) {
						s.next(r);
					}
				} finally {
					lock.unlock();
				}
				
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				lock.lock();
				try {
					for (Observer<Right> or : leftMap.values()) {
						or.error(ex);
					}
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
					observer.get().finish();
					ResultSink.this.closeSilently();
				} finally {
					lock.unlock();
				}
				Closeables.closeSilently(handle);
			}
			/** The left duration observer. */
			class LeftDurationObserver implements Observer<LeftDuration> {
				/** The identifier of the window. */
				protected final int id;
				/** The right value observer. */
				protected final Observer<Right> gr;
				/** The self-close handle. */
				protected final Closeable self;
				/**
				 * Constructor.
				 * @param id The identifier of the window
				 * @param gr The right value observer.
				 * @param self The self-close handle.
				 */
				public LeftDurationObserver(int id, Observer<Right> gr, Closeable self) {
					this.id = id;
					this.gr = gr;
					this.self = self;
				}
				@Override
				public void error(@Nonnull Throwable ex) {
					LeftObserver.this.error(ex);
				}
				@Override
				public void finish() {
					expire(id, gr, self);
				}
				@Override
				public void next(LeftDuration value) {
					expire(id, gr, self);
				}
			}
		}
		/** The right value observer. */
		class RightObserver implements Observer<Right> {
			/** The self-closing handle. */
			protected final Closeable handle;
			/**
			 * Constructor.
			 * @param handle the self-closing handle
			 */
			public RightObserver(Closeable handle) {
				this.handle = handle;
			}
			/**
			 * Terimnate the right window.
			 * @param id the window id
			 * @param resource the resource to close
			 */
			protected void expire(int id, Closeable resource) {
				lock.lock();
				try {
					rightMap.remove(id);
				} finally {
					lock.unlock();
				}
				group.removeSilently(resource);
			}
			@Override
			public void next(Right value) {
				// TODO Auto-generated method stub
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
				try {
					duration = rightDurationSelector.invoke(value);
				} catch (Throwable t) {
					error(t);
					return;
				}
				
				md.set(Observers.registerSafe(duration, new RightDurationObserver(id, md)));
				
				lock.lock();
				try {
					for (Observer<Right> o : leftMap.values()) {
						o.next(value);
					}
				} finally {
					lock.unlock();
				}
			}
			/**
			 * The right window termination observer.
			 * @author akarnokd, 2013.01.16.
			 */
			class RightDurationObserver implements Observer<RightDuration> {
				/** The right window id. */
				protected final int id;
				/** The self closeable. */
				protected final Closeable self;
				/**
				 * Constructor.
				 * @param id the right window id
				 * @param self the self closeable
				 */
				public RightDurationObserver(int id, Closeable self) {
					this.id = id;
					this.self = self;
				}
				@Override
				public void error(@Nonnull Throwable ex) {
					RightObserver.this.error(ex);
				}
				@Override
				public void finish() {
					expire(id, self);
				}
				@Override
				public void next(RightDuration value) {
					expire(id, self);
				}
			}
			@Override
			public void error(@Nonnull Throwable ex) {
				lock.lock();
				try {
					for (Observer<Right> o : leftMap.values()) {
						o.error(ex);
					}
					observer.get().error(ex);
					ResultSink.this.closeSilently();
				} finally {
					lock.unlock();
				}
			}
			@Override
			public void finish() {
				Closeables.closeSilently(handle);
			}
		}
	}
}
