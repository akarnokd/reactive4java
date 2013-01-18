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
package hu.akarnokd.reactive4java.test;

import java.io.Closeable;
import java.io.IOException;

/**
 * A closeable instance which remembers if it was closed.
 * Can be used to test closeability.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class DebugCloseable implements Closeable {
	/** The state. */
	protected volatile boolean done;
	@Override
	public void close() throws IOException {
		done = true;
	}
	/** @return true if the close() method was already called. */
	public boolean isClosed() {
		return done;
	}
}
