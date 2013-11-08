/*
 * Copyright 2013 akarnokd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactive4java8.base;

/**
 * Interface representing a registration to an observable sequence.
 * Can be used in try-with-resources constructs to cancel a registration.
 * <p>It extends the {link AutoCloseable} interface and hides the
 * default {@link Exception} type.</p>
 * <p>reactive4java note: the previous library used {@link java.io.Closeable}
 * since it was the only library that supported try-with-resources and
 * was avaliable in Java 6 as well.</p>
 * @author akarnokd
 */
@FunctionalInterface
public interface Registration extends AutoCloseable {
    @Override
    void close();
}
