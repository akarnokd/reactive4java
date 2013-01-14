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

import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Subject;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.Unique;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Helper class for the Reactive.window operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Windowing {
	/** Helper class. */
	private Windowing() { }
	/**
	 * Projects the parts of the source into non-overlapping observable
	 * sequences until the window observable finishes.
	 * If the window finishes, a new window is started along with a new
	 * observable in the output
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 */
	public static class WithClosing<T, U> implements Observable<Observable<T>> {
		/** The source observable. */
		@Nonnull
		protected final Observable<? extends T> source;
		/** The window closing selector for each registerer. */
		@Nonnull
		protected final Func0<? extends Observable<U>> windowClosingSelector;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param windowClosingSelector the window selector
		 */
		public WithClosing(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Func0<? extends Observable<U>> windowClosingSelector) {
			this.source = source;
			this.windowClosingSelector = windowClosingSelector;
			
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
				/** The current open window. */
				@GuardedBy("lock")
				Subject<T, T> window;

				@Override
				protected void onNext(T value) {
					window.next(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					window.error(ex);
					observer.error(ex);
				}

				@Override
				protected void onFinish() {
					window.finish();
					observer.finish();
				}
				/** Create a new observable window. */
				protected void newWindow() {
					lock.lock();
					try {
						window = new DefaultObservable<T>();
						observer.next(window);
					} finally {
						lock.unlock();
					}
				}
				/**
				 * Callout from the inner observable.
				 * @param ex the exception
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Call init from sub-observers. */
				protected void innerInit() {
					init();
				}
				@Override
				public void init() {
					newWindow();
					DefaultObserverEx<U> closeObserver = new DefaultObserverEx<U>(lock, true) {
						@Override
						protected void onNext(U value) {
							// ignored
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							window.finish();
							innerInit();
						}
						
					};
					
					add("windowClosing", closeObserver.registerWith(windowClosingSelector.invoke()));
				}
			};
			return obs.registerWith(source);
		}
	}
	/**
	 * Projects the source elements into multiple, potentially overlapping
	 * windows. The window is opened by an element
	 * from the windowOpenings sequence and closed once the
	 * associated windowClosingSelector's value finishes (i.e., if it produces elements,
	 * they are ignored).
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window opening element type
	 * @param <V> the window closing element type.
	 */
	public static class WithOpenClose<T, U, V> implements Observable<Observable<T>> {
		/** */
		protected Observable<? extends T> source;
		/** */
		protected Observable<? extends U> windowOpenings;
		/** */
		protected Func1<? super U, ? extends Observable<V>> windowClosingSelector;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param windowOpenings the sequence producing the window-open events
		 * @param windowClosingSelector the selector producing a window-closing sequence for
		 * the pair window-open event.
		 */
		public WithOpenClose(
				@Nonnull Observable<? extends T> source,
				@Nonnull Observable<? extends U> windowOpenings,
				@Nonnull Func1<? super U, ? extends Observable<V>> windowClosingSelector
				) {
			this.source = source;
			this.windowOpenings = windowOpenings;
			this.windowClosingSelector = windowClosingSelector;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			/** The source observer instance. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The open windows map. */
				@GuardedBy("lock")
				Map<Unique<U>, Subject<T, T>> openMap = new HashMap<Unique<U>, Subject<T, T>>();
				
				@Override
				protected void onNext(T value) {
					for (Subject<T, T> s : openMap.values()) {
						s.next(value);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					for (Subject<T, T> s : openMap.values()) {
						s.error(ex);
					}
					observer.error(ex);
					openMap = new HashMap<Unique<U>, Subject<T, T>>();
				}
				/**
				 * Relay exceptions from inner observers.
				 * @param ex the exception
				 */
				protected void innerError(Throwable ex) {
					error(ex);
				}
				@Override
				protected void onFinish() {
					for (Subject<T, T> s : openMap.values()) {
						s.finish();
					}
					observer.finish();
					openMap = new HashMap<Unique<U>, Subject<T, T>>();
				}
				@Override
				public void init() {
					/** Observer for window open values. */
					class OpenObserver extends DefaultObserverEx<U> {
						/**
						 * Constructor.
						 * @param lock the shared lock
						 * @param terminate terminate on finish?
						 */
						public OpenObserver(Lock lock, boolean terminate) {
							super(lock, terminate);
						}
						@Override
						protected void onNext(U value) {
							final Unique<U> token = Unique.of(value);
							final Observable<V> cl = windowClosingSelector.invoke(value);
							
							final Subject<T, T> s = new DefaultObservable<T>();
							
							openMap.put(token, s);
							
							observer.next(s);
							/** The window finish observer. */
							class FinishObserver extends DefaultObserverEx<V> {
								/**
								 * Constructor.
								 * @param lock the shared lock
								 */
								public FinishObserver(Lock lock) {
									super(lock, true);
								}
								@Override
								protected void onNext(V value) {
									// ignored
								}

								@Override
								protected void onError(Throwable ex) {
									innerError(ex);
								}

								@Override
								protected void onFinish() {
									Subject<T, T> s = openMap.remove(token);
									s.finish();
								}
								
							}
							FinishObserver fo = new FinishObserver(lock);
							add(token, fo.registerWith(cl));
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							remove(this);
						}
						
					}
					add("windowOpening", new OpenObserver(lock, false).registerWith(windowOpenings));
				}
			}
			SourceObserver o = new SourceObserver();
			return o.registerWith(source);
		}
	}
	/**
	 * Projects the source elements into a non-overlapping consecutive windows.
	 * <p>The first window opens immediately, The current window is closed when 
	 * the boundary observable sequence has sent a value. The finish
	 * of the boundary will finish both inner and outer observables.
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param <U> the window boundary element type (irrelevant
	 */
	public static class WithBoundary<T, U> implements Observable<Observable<T>> {
		/** The soruce sequence. */
		protected Observable<? extends T> source;
		/** The boundary sequence. */
		protected Observable<U> boundary;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param boundary the window boundary indicator.
		 */
		public WithBoundary(
				@Nonnull Observable<? extends T> source, 
				@Nonnull Observable<U> boundary) {
			this.source = source;
			this.boundary = boundary;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Observable<T>> observer) {
			/** The source observer. */
			class SourceObserver extends DefaultObserverEx<T> {
				/** The current window. */
				@GuardedBy("lock")
				Subject<T, T> window;
				@Override
				protected void onNext(T value) {
					window.next(value);
				}

				@Override
				protected void onError(Throwable ex) {
					window.error(ex);
					observer.error(ex);
				}
				/** 
				 * Called from inner.
				 * @param ex the inner exception 
				 */
				protected void innerError(@Nonnull Throwable ex) {
					error(ex);
				}
				/** Called from inner. */
				protected void innerFinish() {
					finish();
				}
				@Override
				protected void onFinish() {
					window.finish();
					observer.finish();
				}
				@Override
				protected void onRegister() {
					/** The boundary observer. */
					class BoundaryObserver extends DefaultObserverEx<U> {
						/**
						 * Constructor with shared lock.
						 * @param lock the shared lock
						 */
						public BoundaryObserver(Lock lock) {
							super(lock, true);
						}

						@Override
						protected void onNext(U value) {
							window.finish();
							window = new DefaultObservable<T>();
							observer.next(window);
						}

						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							innerFinish();
						}
					}
					lock.lock();
					try {
						window = new DefaultObservable<T>();
						observer.next(window);
					} finally {
						lock.unlock();
					}
					add("boundary", new BoundaryObserver(lock).registerWith(boundary));
				}
			}
			return (new SourceObserver()).registerWith(source);
		}
	}
}
