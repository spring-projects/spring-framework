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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Prepared statement setter that logs values if enabled
 *
 * @author David Thexton
 * @since May 10, 2011
 */
public class PreparedStatementValueLogger implements PreparedStatementSetter {

	protected final Log logger = LogFactory.getLog(getClass());
	private PreparedStatementSetter delegate;


	/**
	 * Create a new prepared statement logging setter.
	 * @param delegate the prepared statement setter delegate
	 */
	public PreparedStatementValueLogger(PreparedStatementSetter delegate) {
		this.delegate = delegate;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		if ((logger.isDebugEnabled()) && (delegate instanceof PreparedStatementValueProvider)) {
			logger.debug("Setting arguments " + ((PreparedStatementValueProvider) delegate).getValuesString());
		}
		delegate.setValues(ps);
	}

}
