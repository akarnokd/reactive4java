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
package hu.akarnokd.reactive4java.reactive;


import java.io.Closeable;

/**
 * A combinational interface for a runnable, closable and an observer, i.e. an observer which has an attached scheduled counterpart and offers the option
 * to close.
 * @author akarnokd, 2011.02.02.
 * @param <T> the element type of the observer
 */
public interface RunnableClosableObserver<T> extends Runnable, Observer<T>, Closeable {

}
