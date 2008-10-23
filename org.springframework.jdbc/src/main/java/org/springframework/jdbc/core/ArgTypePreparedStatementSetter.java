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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Simple adapter for PreparedStatementSetter that applies
 * given arrays of arguments and JDBC argument types.
 *
 * @author Juergen Hoeller
 */
class ArgTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	private final Object[] args;

	private final int[] argTypes;


	/**
	 * Create a new ArgTypePreparedStatementSetter for the given arguments.
	 * @param args the arguments to set
	 * @param argTypes the corresponding SQL types of the arguments
	 */
	public ArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
				(args != null && args.length != argTypes.length)) {
			throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}


	public void setValues(PreparedStatement ps) throws SQLException {
		int argIndx = 1;
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
					Collection entries = (Collection) arg;
					for (Iterator it = entries.iterator(); it.hasNext();) {
						Object entry = it.next();
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[])entry);
							for (int k = 0; k < valueArray.length; k++) {
								Object argValue = valueArray[k];
								StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], argValue);
							}
						}
						else {
							StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], entry);
						}
					}
				}
				else {
					StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], arg);
				}
			}
		}
	}

	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
