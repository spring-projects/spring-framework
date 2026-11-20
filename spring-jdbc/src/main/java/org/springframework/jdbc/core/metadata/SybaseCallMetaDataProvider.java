/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * Sybase specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Giuseppe Milicia
 * @since 2.5
 */
public class SybaseCallMetaDataProvider extends GenericCallMetaDataProvider {

	private static final String REMOVABLE_COLUMN_PREFIX = "@";

	private static final String RETURN_VALUE_NAME = "RETURN_VALUE";


	public SybaseCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public @Nullable String parameterNameToUse(@Nullable String parameterName) {
		if (parameterName == null) {
			return null;
		}
		else if (parameterName.length() > 1 && parameterName.startsWith(REMOVABLE_COLUMN_PREFIX)) {
			return super.parameterNameToUse(parameterName.substring(1));
		}
		else {
			return super.parameterNameToUse(parameterName);
		}
	}

	@Override
	public String namedParameterBindingToUse(@Nullable String parameterName) {
		return parameterName + " = ?";
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return (RETURN_VALUE_NAME.equals(parameterName) ||
				RETURN_VALUE_NAME.equals(parameterNameToUse(parameterName)));
	}

}
