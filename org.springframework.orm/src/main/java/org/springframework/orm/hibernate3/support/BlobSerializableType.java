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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Hibernate UserType implementation for arbitrary objects that get serialized to BLOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>Can also be defined in generic Hibernate mappings, as DefaultLobCreator will
 * work with most JDBC-compliant database drivers. In this case, the field type
 * does not have to be BLOB: For databases like MySQL and MS SQL Server, any
 * large enough binary type will work.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#setLobHandler
 */
public class BlobSerializableType extends AbstractLobType {

	/**
	 * Initial size for ByteArrayOutputStreams used for serialization output.
	 * <p>If a serialized object is larger than these 1024 bytes, the size of
	 * the byte array used by the output stream will be doubled each time the
	 * limit is reached.
	 */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 1024;

	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeLobHandler
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeTransactionManager
	 */
	public BlobSerializableType() {
		super();
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be <code>null</code>).
	 */
	protected BlobSerializableType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		super(lobHandler, jtaTransactionManager);
	}

	public int[] sqlTypes() {
		return new int[] {Types.BLOB};
	}

	public Class returnedClass() {
		return Serializable.class;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		try {
			// Write to new byte array to clone.
			ByteArrayOutputStream baos = new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			try {
				oos.writeObject(value);
			}
			finally {
				oos.close();
			}

			// Read it back and return a true copy.
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			try {
				return ois.readObject();
			}
			finally {
				ois.close();
			}
		}
		catch (ClassNotFoundException ex) {
			throw new HibernateException("Couldn't clone BLOB contents", ex);
		}
		catch (IOException ex) {
			throw new HibernateException("Couldn't clone BLOB contents", ex);
		}
	}

	@Override
	protected Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException, IOException, HibernateException {

		InputStream is = lobHandler.getBlobAsBinaryStream(rs, names[0]);
		if (is != null) {
			ObjectInputStream ois = new ObjectInputStream(is);
			try {
				return ois.readObject();
			}
			catch (ClassNotFoundException ex) {
				throw new HibernateException("Could not deserialize BLOB contents", ex);
			}
			finally {
				ois.close();
			}
		}
		else {
			return null;
		}
	}

	@Override
	protected void nullSafeSetInternal(
			PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException, IOException {

		if (value != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			try {
				oos.writeObject(value);
				oos.flush();
				lobCreator.setBlobAsBytes(ps, index, baos.toByteArray());
			}
			finally {
				oos.close();
			}
		}
		else {
			lobCreator.setBlobAsBytes(ps, index, null);
		}
	}

}
