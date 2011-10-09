/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.orm.hibernate3.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.transaction.TransactionManager;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Hibernate UserType implementation for Strings that get mapped to CLOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>Particularly useful for storing Strings with more than 4000 characters in an
 * Oracle database (only possible via CLOBs), in combination with OracleLobHandler.
 *
 * <p>Can also be defined in generic Hibernate mappings, as DefaultLobCreator will
 * work with most JDBC-compliant database drivers. In this case, the field type
 * does not have to be CLOB: For databases like MySQL and MS SQL Server, any
 * large enough character type will work.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#setLobHandler
 */
public class ClobStringType extends AbstractLobType {

	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeLobHandler
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeTransactionManager
	 */
	public ClobStringType() {
		super();
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be <code>null</code>).
	 */
	protected ClobStringType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		super(lobHandler, jtaTransactionManager);
	}

	public int[] sqlTypes() {
		return new int[] {Types.CLOB};
	}

	public Class returnedClass() {
		return String.class;
	}

	@Override
	protected Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException {

		return lobHandler.getClobAsString(rs, names[0]);
	}

	@Override
	protected void nullSafeSetInternal(
			PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException {

		lobCreator.setClobAsString(ps, index, (String) value);
	}

}
