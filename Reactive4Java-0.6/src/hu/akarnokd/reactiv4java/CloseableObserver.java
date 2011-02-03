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

import java.io.Closeable;

/**
 * A simple helper interface for a closable and observable instance.
 * @author akarnokd, 2011.02.02.
 * @param <T> the element type to observe
 */
public interface CloseableObserver<T> extends Observer<T>, Closeable {

}