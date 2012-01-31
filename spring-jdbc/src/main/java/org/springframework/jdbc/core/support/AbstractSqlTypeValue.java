/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.SqlTypeValue;

/**
 * Abstract implementation of the SqlTypeValue interface, for convenient
 * creation of type values that are supposed to be passed into the
 * <code>PreparedStatement.setObject</code> method. The <code>createTypeValue</code>
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

	public final void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName)
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
	 * Create the type value to be passed into <code>PreparedStatement.setObject</code>.
	 * @param con the JDBC Connection, if needed to create any database-specific objects
	 * @param sqlType SQL type of the parameter we are setting
	 * @param typeName the type name of the parameter
	 * @return the type value
	 * @throws SQLException if a SQLException is encountered setting
	 * parameter values (that is, there's no need to catch SQLException)
	 * @see java.sql.PreparedStatement#setObject(int, Object, int)
	 */
	protected abstract Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException;

}
