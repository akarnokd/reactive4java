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

package hu.akarnokd.tools;

/**
 * Hang the double conversion.
 * @author karnokd, 2011.02.09.
 */
public final class Runhang {
	/** Utility class. */
	private Runhang() {
		
	}
	/**
	 * @param args no arguments
	 */
	public static void main(String[] args) {
		System.out.println("Test:");
		double d = Double.parseDouble("2.2250738585072012e-308");
		System.out.println("Value: " + d);
	}
}
