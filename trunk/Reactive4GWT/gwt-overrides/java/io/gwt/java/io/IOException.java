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
package java.io;

/**
 * The GWT version of the java.io.IOException.
 * @author karnok, 2013.01.17.
 */
public class IOException extends Exception {
	/** Default constructor. */
	public IOException() {
		super();
	}
	/**
	 * Constructor with message.
	 * @param message the message
	 */
	public IOException(String message) {
		super(message);
	}
	/**
	 * Constructor with cause exception.
	 * @param cause the cause
	 */
	public IOException(Throwable cause) {
		super(cause);
	}
	/**
	 * Constructor with message and cause.
	 * @param message the message
	 * @param cause the cause
	 */
	public IOException(String message, Throwable cause) {
		super(message, cause);
	}
}