/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

/**
 * Class to generate {@link SQLException} subclasses for testing purposes.
 *
 * @author Thomas Risberg
 */
public class SQLExceptionSubclassFactory {

	public static SQLException newSQLDataException(String reason, String SQLState, int vendorCode) {
		return new SQLDataException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLFeatureNotSupportedException(String reason, String SQLState, int vendorCode) {
		return new SQLFeatureNotSupportedException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLIntegrityConstraintViolationException(String reason, String SQLState, int vendorCode) {
		return new SQLIntegrityConstraintViolationException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLInvalidAuthorizationSpecException(String reason, String SQLState, int vendorCode) {
		return new SQLInvalidAuthorizationSpecException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLNonTransientConnectionException(String reason, String SQLState, int vendorCode) {
		return new SQLNonTransientConnectionException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLSyntaxErrorException(String reason, String SQLState, int vendorCode) {
		return new SQLSyntaxErrorException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLTransactionRollbackException(String reason, String SQLState, int vendorCode) {
		return new SQLTransactionRollbackException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLTransientConnectionException(String reason, String SQLState, int vendorCode) {
		return new SQLTransientConnectionException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLTimeoutException(String reason, String SQLState, int vendorCode) {
		return new SQLTimeoutException(reason, SQLState, vendorCode);
	}

	public static SQLException newSQLRecoverableException(String reason, String SQLState, int vendorCode) {
		return new SQLRecoverableException(reason, SQLState, vendorCode);
	}

}
