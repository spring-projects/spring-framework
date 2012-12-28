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

package org.springframework.orm.ibatis.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @since 27.02.2005
 */
public class LobTypeHandlerTests extends TestCase {

	private MockControl rsControl = MockControl.createControl(ResultSet.class);
	private ResultSet rs = (ResultSet) rsControl.getMock();
	private MockControl psControl = MockControl.createControl(PreparedStatement.class);
	private PreparedStatement ps = (PreparedStatement) psControl.getMock();

	private MockControl lobHandlerControl = MockControl.createControl(LobHandler.class);
	private LobHandler lobHandler = (LobHandler) lobHandlerControl.getMock();
	private MockControl lobCreatorControl = MockControl.createControl(LobCreator.class);
	private LobCreator lobCreator = (LobCreator) lobCreatorControl.getMock();

	@Override
	protected void setUp() throws SQLException {
		rs.findColumn("column");
		rsControl.setReturnValue(1);

		lobHandler.getLobCreator();
		lobHandlerControl.setReturnValue(lobCreator);
		lobCreator.close();
		lobCreatorControl.setVoidCallable(1);

		rsControl.replay();
		psControl.replay();
	}

	public void testClobStringTypeHandler() throws Exception {
		lobHandler.getClobAsString(rs, 1);
		lobHandlerControl.setReturnValue("content", 2);
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
		assertEquals("content", type.valueOf("content"));
		assertEquals("content", type.getResult(rs, "column"));
		assertEquals("content", type.getResult(rs, 1));

		TransactionSynchronizationManager.initSynchronization();
		try {
			type.setParameter(ps, 1, "content", null);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			assertTrue(synchs.get(0).getClass().getName().endsWith("LobCreatorSynchronization"));
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testClobStringTypeWithSynchronizedConnection() throws Exception {
		DataSource dsTarget = new DriverManagerDataSource();
		DataSource ds = new LazyConnectionDataSourceProxy(dsTarget);

		lobHandler.getClobAsString(rs, 1);
		lobHandlerControl.setReturnValue("content", 2);
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
		assertEquals("content", type.valueOf("content"));
		assertEquals("content", type.getResult(rs, "column"));
		assertEquals("content", type.getResult(rs, 1));

		TransactionSynchronizationManager.initSynchronization();
		try {
			DataSourceUtils.getConnection(ds);
			type.setParameter(ps, 1, "content", null);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(2, synchs.size());
			assertTrue(synchs.get(0).getClass().getName().endsWith("LobCreatorSynchronization"));
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			((TransactionSynchronization) synchs.get(1)).beforeCompletion();
			((TransactionSynchronization) synchs.get(1)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobByteArrayType() throws Exception {
		byte[] content = "content".getBytes();
		lobHandler.getBlobAsBytes(rs, 1);
		lobHandlerControl.setReturnValue(content, 2);
		lobCreator.setBlobAsBytes(ps, 1, content);
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobByteArrayTypeHandler type = new BlobByteArrayTypeHandler(lobHandler);
		assertTrue(Arrays.equals(content, (byte[]) type.valueOf("content")));
		assertEquals(content, type.getResult(rs, "column"));
		assertEquals(content, type.getResult(rs, 1));

		TransactionSynchronizationManager.initSynchronization();
		try {
			type.setParameter(ps, 1, content, null);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobSerializableType() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		lobHandler.getBlobAsBinaryStream(rs, 1);
		lobHandlerControl.setReturnValue(new ByteArrayInputStream(baos.toByteArray()), 1);
		lobHandler.getBlobAsBinaryStream(rs, 1);
		lobHandlerControl.setReturnValue(new ByteArrayInputStream(baos.toByteArray()), 1);
		lobCreator.setBlobAsBytes(ps, 1, baos.toByteArray());
		lobCreatorControl.setMatcher(new ArgumentsMatcher() {
			@Override
			public boolean matches(Object[] o1, Object[] o2) {
				return Arrays.equals((byte[]) o1[2], (byte[]) o2[2]);
			}
			@Override
			public String toString(Object[] objects) {
				return null;
			}
		});

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableTypeHandler type = new BlobSerializableTypeHandler(lobHandler);
		assertEquals("content", type.valueOf("content"));
		assertEquals("content", type.getResult(rs, "column"));
		assertEquals("content", type.getResult(rs, 1));

		TransactionSynchronizationManager.initSynchronization();
		try {
			type.setParameter(ps, 1, "content", null);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobSerializableTypeWithNull() throws Exception {
		lobHandler.getBlobAsBinaryStream(rs, 1);
		lobHandlerControl.setReturnValue(null, 2);
		lobCreator.setBlobAsBytes(ps, 1, null);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableTypeHandler type = new BlobSerializableTypeHandler(lobHandler);
		assertEquals(null, type.valueOf(null));
		assertEquals(null, type.getResult(rs, "column"));
		assertEquals(null, type.getResult(rs, 1));

		TransactionSynchronizationManager.initSynchronization();
		try {
			type.setParameter(ps, 1, null, null);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Override
	protected void tearDown() {
		try {
			rsControl.verify();
			psControl.verify();
			lobHandlerControl.verify();
			lobCreatorControl.verify();
		}
		catch (IllegalStateException ex) {
			// ignore: test method didn't call replay
		}
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

}
