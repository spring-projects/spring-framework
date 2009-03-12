/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.jdbc.support;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.sql.SQLException;

/**
 * Custom SQLException translation for testing.
 *
 * @author Thomas Risberg
 */
public class CustomSqlExceptionTranslator implements SQLExceptionTranslator {
	public DataAccessException translate(String task, String sql, SQLException ex) {
		if (ex.getErrorCode() == 2) {
			return new TransientDataAccessResourceException("Custom", ex);
		}
		return null;
	}
}
