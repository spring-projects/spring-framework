/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.transaction.TransactionManager;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Hibernate UserType implementation for Strings that get mapped to BLOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>This is intended for the (arguably unnatural, but still common) case
 * where character data is stored in a binary LOB. This requires encoding
 * and decoding the characters within this UserType; see the javadoc of the
 * {@code getCharacterEncoding()} method.
 *
 * <p>Can also be defined in generic Hibernate mappings, as DefaultLobCreator will
 * work with most JDBC-compliant database drivers. In this case, the field type
 * does not have to be BLOB: For databases like MySQL and MS SQL Server, any
 * large enough binary type will work.
 *
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see #getCharacterEncoding()
 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#setLobHandler
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class BlobStringType extends AbstractLobType {

	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeLobHandler
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeTransactionManager
	 */
	public BlobStringType() {
		super();
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be {@code null}).
	 */
	protected BlobStringType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		super(lobHandler, jtaTransactionManager);
	}

	@Override
	public int[] sqlTypes() {
		return new int[] {Types.BLOB};
	}

	@Override
	public Class<?> returnedClass() {
		return String.class;
	}

	@Override
	protected Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException, UnsupportedEncodingException {

		byte[] bytes = lobHandler.getBlobAsBytes(rs, names[0]);
		if (bytes != null) {
			String encoding = getCharacterEncoding();
			return (encoding != null ? new String(bytes, encoding) : new String(bytes));
		}
		else {
			return null;
		}
	}

	@Override
	protected void nullSafeSetInternal(
			PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException, UnsupportedEncodingException {

		if (value != null) {
			String str = (String) value;
			String encoding = getCharacterEncoding();
			byte[] bytes = (encoding != null ? str.getBytes(encoding) : str.getBytes());
			lobCreator.setBlobAsBytes(ps, index, bytes);
		}
		else {
			lobCreator.setBlobAsBytes(ps, index, null);
		}
	}

	/**
	 * Determine the character encoding to apply to the BLOB's bytes
	 * to turn them into a String.
	 * <p>Default is {@code null}, indicating to use the platform
	 * default encoding. To be overridden in subclasses for a specific
	 * encoding such as "ISO-8859-1" or "UTF-8".
	 * @return the character encoding to use, or {@code null}
	 * to use the platform default encoding
	 * @see String#String(byte[], String)
	 * @see String#getBytes(String)
	 */
	protected String getCharacterEncoding() {
		return null;
	}

}
