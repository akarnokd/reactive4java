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
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.R4JConfig;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * The configuration manager for this library.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public final class R4JConfigManager {
	/** Just static methods. */
	private R4JConfigManager() { }
	/** The global configuration. */
	private static final AtomicReference<R4JConfig> GLOBAL_CONFIG = new AtomicReference<R4JConfig>(new Default());









	/**
	 * @return
	 * Returns the configuration for the current thread or the global configuration, if
	 * no local is present.
	 */
	public static R4JConfig get() {








		return GLOBAL_CONFIG.get();

	}










	/**
	 * Sets the global configuration.
	 * @param config the global configuration
	 */
	public static void setGlobal(@Nonnull R4JConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("config is null");
		}
		GLOBAL_CONFIG.set(config);
	}
	/**
	 * The default settings for the configuration.
	 * @author akarnokd, 2013.01.16.
	 */
	private static final class Default implements R4JConfig {
		@Override
		public boolean useFairLocks() {
			return true;
		}
		@Override
		public Action1<? super IOException> silentExceptionHandler() {
			return null;
		}
	}
}
