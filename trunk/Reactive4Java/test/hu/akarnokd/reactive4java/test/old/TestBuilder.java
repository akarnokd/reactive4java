/*
 * Copyright 2011-2012 David Karnok
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
package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Lambdas;
import hu.akarnokd.reactive4java.base.Pred1;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.query.IterableBuilder;
import hu.akarnokd.reactive4java.query.ObservableBuilder;

/**
 * Testing the Builder version of the framework.
 * @author akarnokd, 2012.01.26.
 */
public final class TestBuilder {

	/** Utility class. */
	private TestBuilder() {
	}

	/**
	 * @param args no arguments
	 */
	public static void main(String[] args) {
		// return a single element
		IterableBuilder.from(1).print();
		// return a range
		IterableBuilder.range(0, 10).print();
		// combine two iterables
		IterableBuilder.from(0).concat(IterableBuilder.range(1, 9)).print();
		// return multiple things for a single thing
		
		IterableBuilder.range(0, 10).selectMany(
		new Func1<Integer, Iterable<Integer>>() {
			@Override
	        public Iterable<Integer> invoke(Integer param) {
	            return Interactive.range(0, param + 1);
	        }
	    }).print();
		
		// classic query
		IterableBuilder.range(0, 10)
		.where(new Pred1<Integer>() {
			@Override
			public Boolean invoke(Integer param1) {
				return param1 % 2 == 0;
			} })
		.select(new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 * param1;
			} })
		.print();
		
		// lambda functions via scripting
		IterableBuilder.range(0, 10).where(
				Lambdas.<Integer, Boolean>js1("o => o % 2 == 0"))
				.print();
		
		// return a single element
		ObservableBuilder.from(1).print();
		// return a range
		ObservableBuilder.range(0, 10).print();
		// combine two iterables
		ObservableBuilder.from(0).concat(ObservableBuilder.range(1, 9)).print();
		// return multiple things for a single thing
		
		ObservableBuilder.range(0, 10).selectMany(
		new Func1<Integer, ObservableBuilder<Integer>>() {
			@Override
	        public ObservableBuilder<Integer> invoke(Integer param) {
	            return ObservableBuilder.range(0, param + 1);
	        }
	    }).print();
		
		// classic query
		ObservableBuilder.range(0, 10)
		.where(new Pred1<Integer>() {
			@Override
			public Boolean invoke(Integer param1) {
				return param1 % 2 == 0;
			} })
		.select(new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 * param1;
			} })
		.print();
		
		// lambda functions via scripting
		ObservableBuilder.range(0, 10).where(
				Lambdas.<Integer, Boolean>js1("o => o % 2 == 0"))
				.print();
	}

}
