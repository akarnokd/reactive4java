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

import java.io.IOException;

/**
 * Interface for global or thread-local configuration of
 * various base objects.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public interface R4JConfig {
    /** @return true if locks used should be fair. */
    boolean useFairLocks();
    /** 
     * @return 
     * The action to invoke on silently closed Closeables' exceptions.
     * May return null indicating a no-op. 
     */
    Action1<? super IOException> silentExceptionHandler();
}
