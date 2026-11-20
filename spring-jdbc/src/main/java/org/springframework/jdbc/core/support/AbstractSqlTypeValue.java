/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.SqlTypeValue;

/**
 * Abstract implementation of the SqlTypeValue interface, for convenient
 * creation of type values that are supposed to be passed into the
 * {@code PreparedStatement.setObject} method. The {@code createTypeValue}
 * callback method has access to the underlying Connection, if that should
 * be needed to create any database-specific objects.
 *
 * <p>A usage example from a StoredProcedure (compare this to the plain
 * SqlTypeValue version in the superclass javadoc):
 *
 * <pre class="code">proc.declareParameter(new SqlParameter("myarray", Types.ARRAY, "NUMBERS"));
 * ...
 *
 * Map&lt;String, Object&gt; in = new HashMap&lt;String, Object&gt;();
 * in.put("myarray", new AbstractSqlTypeValue() {
 *   public Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
 *	   oracle.sql.ArrayDescriptor desc = new oracle.sql.ArrayDescriptor(typeName, con);
 *	   return new oracle.sql.ARRAY(desc, con, seats);
 *   }
 * });
 * Map out = execute(in);
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.sql.PreparedStatement#setObject(int, Object, int)
 * @see org.springframework.jdbc.object.StoredProcedure
 */
public abstract class AbstractSqlTypeValue implements SqlTypeValue {

	@Override
	public final void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		Object value = createTypeValue(ps.getConnection(), sqlType, typeName);
		if (sqlType == TYPE_UNKNOWN) {
			ps.setObject(paramIndex, value);
		}
		else {
			ps.setObject(paramIndex, value, sqlType);
		}
	}

	/**
	 * Create the type value to be passed into {@code PreparedStatement.setObject}.
	 * @param con the JDBC Connection, if needed to create any database-specific objects
	 * @param sqlType the SQL type of the parameter we are setting
	 * @param typeName the type name of the parameter
	 * @return the type value
	 * @throws SQLException if an SQLException is encountered setting
	 * parameter values (that is, there's no need to catch SQLException)
	 * @see java.sql.PreparedStatement#setObject(int, Object, int)
	 */
	protected abstract Object createTypeValue(Connection con, int sqlType, @Nullable String typeName)
			throws SQLException;

}
