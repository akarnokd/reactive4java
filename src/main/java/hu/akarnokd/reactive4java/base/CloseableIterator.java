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

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator which supports a close() method. Usage:
 * <pre><code>
 * CloseableIterator<T> it = citerable.iterator();
 * try {
 *     while (it.hasNext()) {
 *         doSomething(it.next());
 *         if (condition) {
 *             break;
 *         }
 *     }
 * } finally {
 *     it.close();
 * }
 * it.close();
 * </code></pre>
 * @author akarnokd, 2011.10.26.
 * @param <T> the element type
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

}
