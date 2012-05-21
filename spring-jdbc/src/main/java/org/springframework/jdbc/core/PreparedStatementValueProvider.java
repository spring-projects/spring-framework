/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.core;

/**
 * Interface intended to be used primarily to assist logging.
 *
 * Those implementing {@link org.springframework.jdbc.core.PreparedStatementSetter}
 * should also implement this interface so that debug logging is possible.
 *
 * @author David Thexton
 * @since May 10, 2011
 */
public interface PreparedStatementValueProvider {

	/**
	 * Return the values to be used in the prepared statement (for logging).
	 */
	String getValuesString();

}
