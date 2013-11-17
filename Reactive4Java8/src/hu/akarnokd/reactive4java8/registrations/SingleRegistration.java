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

package hu.akarnokd.reactive4java8.registrations;

import hu.akarnokd.reactive4java8.Registration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A registration which maintains a single sub-registration
 * which can be replaced via set() method.
 * The previous registration is then closed.
 * @author akarnokd, 2013.11.09.
 */
public class SingleRegistration implements Registration {
    /** The managed registration. */
    private final AtomicReference<Registration> reg;
    /** The closed registration token. */
    private static final Registration SENTINEL = () -> { };
    /**
     * Default constructor, empty managed registration.
     */
    public SingleRegistration() {
        this.reg = new AtomicReference<>();
    }
    /**
     * Constructor with an initial registration to maintain.
     * @param reg 
     */
    public SingleRegistration(Registration reg) {
        this.reg = new AtomicReference<>(Objects.requireNonNull(reg));
    }
    /**
     * Set a new registration and close the original one.
     * @param newReg the new registration
     */
    public void set(Registration newReg) {
        Registration q = null;
        do {
            Registration r = reg.get();
            if (r == SENTINEL) {
                q = newReg;
                break;
            }
            if (reg.compareAndSet(r, newReg)) {
                q = r;
                break;
            }
        } while (true);
        if (q != null) {
            q.close();
        }
    }

    @Override
    public void close() {
        Registration r = reg.getAndSet(SENTINEL);
        if (r != null) {
            r.close();
        }
    }
    /**
     * Is this registration closed?
     * @return 
     */
    public boolean isClosed() {
        return reg.get() == SENTINEL;
    }
    /**
     * Returns the current contained registration.
     * @return 
     */
    public Registration get() {
        Registration r = reg.get();
        if (r == SENTINEL) {
            return Registration.EMPTY;
        }
        return r;
    }
}
