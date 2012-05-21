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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Prepared statement result logger logs results if enabled.
 *
 * @author David Thexton
 * @since May 10, 2011
 * @param <T> the result type
 */
public class PreparedStatementResultLogger<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Log result if enabled.
	 * @param result the result
	 */
    public void log(T result) {
		if (logger.isDebugEnabled()) {
			logger.debug("Result " + result);
		}
	}

}
