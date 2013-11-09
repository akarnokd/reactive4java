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

import java.util.Objects;

/**
 * A registration which maintains a single sub-registration
 * which can be replaced via set() method.
 * The previous registration is then closed.
 * @author akarnokd, 2013.11.09.
 */
public class SingleRegistration extends BaseRegistration {
    private Registration reg;
    public SingleRegistration() {
        super();
    }
    public SingleRegistration(Registration reg) {
        super();
        reg = Objects.requireNonNull(reg);
    }
    public void set(Registration newReg) {
        Registration toClose = ls.sync(() -> {
            if (!done) {
                Registration r = reg;
                reg = newReg;
                return r;
            }
            return newReg;
        });
        if (toClose != null) {
            toClose.close();
        }
    }
    /**
     * Removes the current maintained registration without closing it.
     */
    public void clear() {
        ls.sync(() -> {
            reg = null;
        });
    }
    @Override
    public void close() {
        Registration toClose = ls.sync(() -> {
            if (!done) {
                done = true;
                Registration r = reg;
                reg = null;
                return r;
            }
            return null;
        });
        if (toClose != null) {
            toClose.close();
        }
    }
    
}
