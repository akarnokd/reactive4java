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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A composite registration which maintains a list of
 * sub-registrations that can be dynamically added and
 * removed.
 * Closing the registration closes all maintained
 * sub-registrations and closes any new registrations
 * that appear.
 * @author akarnokd, 2013.11.09.
 */
public class CompositeRegistration extends BaseRegistration {
    /** The list of registrations. */
    private List<Registration> list;
    /**
     * Constructor with empty list of registrations.
     */
    public CompositeRegistration() {
        super();
        this.list = new LinkedList<>();
    }
    /**
     * Constructor with optional starting registrations.
     * @param regs 
     */
    public CompositeRegistration(Registration... regs) {
        super();
        Objects.requireNonNull(regs);
        list = new ArrayList<>(regs.length + 1);
        for (Registration reg : regs) {
            list.add(Objects.requireNonNull(reg));
        }
    }
    /**
     * Constructor with a sequence of starting registrations.
     * @param regs 
     */
    public CompositeRegistration(Iterable<? extends Registration> regs) {
        super();
        Objects.requireNonNull(regs);
        regs.forEach((v) -> list.add(Objects.requireNonNull(v)));
    }
    /**
     * Add a new registration to this composite.
     * @param reg 
     */
    public void add(Registration reg) {
        Objects.requireNonNull(reg);
        if (ls.sync(() -> {
            boolean r = done;
            if (!r) {
                list.add(reg);
            }
            return r;
        })) {
            reg.close();
        }
    }
    /**
     * Adds all elements to this composite unless the current
     * state is already done.
     * @param regs 
     */
    public void addAll(Iterable<? extends Registration> regs) {
        Objects.requireNonNull(regs);
        if (ls.sync(() -> {
            boolean r = done;
            if (!r) {
                regs.forEach(list::add);
            }
            return r;
        })) {
            regs.forEach(Registration::close);
        }
    }
    /**
     * Adds all elements to this composite unless the current
     * state is already done.
     * @param regs 
     */
    public void addAll(Registration... regs) {
        addAll(Arrays.asList(regs));
    }
    /**
     * Removes the given registration from this composite,
     * but does not close it.
     * @param reg the registration to remove
     */
    public void remove(Registration reg) {
        ls.sync(() -> { if (!done) { list.remove(reg); } });
    }
    /**
     * Removes and closes the given registration.
     * @param reg 
     */
    public void close(Registration reg) {
        remove(reg);
        if (reg != null) {
            reg.close();
        }
    }
    /**
     * Clears the contents of this composite registration.
     * Does not close cleared registrations.
     */
    public void clear() {
        ls.sync(() -> { if (!done) { list = new ArrayList<>(); } });
    }
    @Override
    public void close() {
        List<Registration> toClose = ls.sync(() -> {
            if (!done) {
                done = true;
                List<Registration> r = new ArrayList<>(list);
                list = null;
                return r;
            }
            return null;
        });
        if (toClose != null) {
            for (Registration r : toClose) {
                try {
                    r.close();
                } catch (Throwable t) {
                    // FIXME ignored???
                }
            }
        }
    }
    /**
     * Returns the number of sub-registrations maintained
     * by this composite.
     * @return the number
     */
    public int size() {
        return ls.sync(() -> list != null ? list.size() : 0);
    }
    /**
     * Returns true if there are no sub-registrations in this composite.
     * @return 
     */
    public boolean isEmpty() {
        return ls.sync(() -> list == null || list.isEmpty());
    }
}
