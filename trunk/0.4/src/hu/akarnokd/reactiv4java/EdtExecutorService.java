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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The wrapper for the event dispatch thread. 
 * @author akarnokd, 2011.01.28.
 */
public final class EdtExecutorService
		implements ExecutorService {
	/** The syncron pool to call invokeAndWait() on EDT in sequence. */
	private static final ExecutorService SYNCHRON_POOL = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	@Override
	public void execute(Runnable command) {
		// TODO Auto-generated method stub
		EventQueue.invokeLater(command);
	}

	@Override
	public void shutdown() {
		
	}

	@Override
	public List<Runnable> shutdownNow() {
		return new ArrayList<Runnable>();
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return false;
	}

	@Override
	public <T> Future<T> submit(final Callable<T> task) {
		return SYNCHRON_POOL.submit(edtInvokeAndWait(task));
	}

	/**
	 * Creates a wrapper callable task which invokes the original task in the EDT
	 * and waits for its completion, then forwards any result or exception as its own.
	 * @param <T> the result type
	 * @param task the original task
	 * @return the wrapper callable
	 */
	<T> Callable<T> edtInvokeAndWait(final Callable<T> task) {
		return new Callable<T>() {
			@Override
			public T call() throws Exception {
				final Ref<T> result = Ref.of();
				final Ref<Exception> exc = Ref.of();
				EventQueue.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						try {
							result.set(task.call());
						} catch (Exception ex) {
							exc.set(ex);
						}
					}
				});
				if (exc.get() != null) {
					throw exc.get();
				}
				return result.get();
			}
		};
	}

	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		return submit(new Callable<T>() {
			@Override
			public T call() throws Exception {
				task.run();
				return result;
			}
		});
	}

	@Override
	public Future<?> submit(Runnable task) {
		return submit(task, null);
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		List<Callable<T>> wrappers = new ArrayList<Callable<T>>();
		for (Callable<T> c : tasks) {
			wrappers.add(edtInvokeAndWait(c));
		}
		return SYNCHRON_POOL.invokeAll(wrappers);
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout,
			TimeUnit unit) throws InterruptedException {
		List<Callable<T>> wrappers = new ArrayList<Callable<T>>();
		for (Callable<T> c : tasks) {
			wrappers.add(edtInvokeAndWait(c));
		}
		return SYNCHRON_POOL.invokeAll(wrappers, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		List<Callable<T>> wrappers = new ArrayList<Callable<T>>();
		for (Callable<T> c : tasks) {
			wrappers.add(edtInvokeAndWait(c));
		}
		return SYNCHRON_POOL.invokeAny(wrappers);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		List<Callable<T>> wrappers = new ArrayList<Callable<T>>();
		for (Callable<T> c : tasks) {
			wrappers.add(edtInvokeAndWait(c));
		}
		return SYNCHRON_POOL.invokeAny(wrappers, timeout, unit);
	}
}
