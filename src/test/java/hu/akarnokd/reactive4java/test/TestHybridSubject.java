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

import hu.akarnokd.reactive4java.util.HybridSubject;

import org.junit.Test;

/**
 * Tests the HybridSubject relay, registration and deregistration behavior.
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 */
public class TestHybridSubject {
    /** 
     * Test an event stream from java to java on the HybridSubject.
     * @throws InterruptedException on interruption 
     */
    @Test(timeout = 1000)
    public void testHybridJavaToJavaIndirect() throws InterruptedException {
        HybridSubject<Integer> subject = new HybridSubject<Integer>();

        TestHybridHelper.testJavaToJava(subject, subject);
    }
    /**
     * Test the java to reactive streaming.
     * @throws InterruptedException on interruption 
     */
    @Test(timeout = 1000)
    public void testHybridJavaToReactive() throws InterruptedException {
        HybridSubject<Integer> subject = new HybridSubject<Integer>();
        TestHybridHelper.testJavaToReactive(subject, subject);
    }
    /**
     * Test the reactive to java streaming.
     * @throws InterruptedException on interruption 
     */
    @Test/* (timeout = 1000) */
    public void testHybridReactiveToJava() throws InterruptedException {
        HybridSubject<Integer> subject = new HybridSubject<Integer>();
        TestHybridHelper.testReactiveToJava(subject, subject);
    }
    /**
     * Test the reactive to reactive streaming.
     * @throws InterruptedException on interruption 
     */
    @Test(timeout = 1000)
    public void testHybridReactiveToReactive() throws InterruptedException {
        HybridSubject<Integer> subject = new HybridSubject<Integer>();

        TestHybridHelper.testReactiveToReactive(subject, subject);
    }
}
