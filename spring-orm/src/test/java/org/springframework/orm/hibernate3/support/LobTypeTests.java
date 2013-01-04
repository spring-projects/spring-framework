/*
 * Copyright 2002-2013 the original author or authors.
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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.easymock.internal.ArrayMatcher;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.tests.transaction.MockJtaTransaction;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
public class LobTypeTests extends TestCase {

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
		lobHandler.getLobCreator();
		lobHandlerControl.setReturnValue(lobCreator);
		lobCreator.close();
		lobCreatorControl.setVoidCallable(1);

		rsControl.replay();
		psControl.replay();
	}

	public void testClobStringType() throws Exception {
		lobHandler.getClobAsString(rs, "column");
		lobHandlerControl.setReturnValue("content");
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringType type = new ClobStringType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.CLOB, type.sqlTypes()[0]);
		assertEquals(String.class, type.returnedClass());
		assertTrue(type.equals("content", "content"));
		assertEquals("content", type.deepCopy("content"));
		assertFalse(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, "content", 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			assertTrue(synchs.get(0).getClass().getName().endsWith("SpringLobCreatorSynchronization"));
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testClobStringTypeWithSynchronizedSession() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		lobHandler.getClobAsString(rs, "column");
		lobHandlerControl.setReturnValue("content");
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringType type = new ClobStringType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.CLOB, type.sqlTypes()[0]);
		assertEquals(String.class, type.returnedClass());
		assertTrue(type.equals("content", "content"));
		assertEquals("content", type.deepCopy("content"));
		assertFalse(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			SessionFactoryUtils.getSession(sf, true);
			type.nullSafeSet(ps, "content", 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(2, synchs.size());
			assertTrue(synchs.get(0).getClass().getName().endsWith("SpringLobCreatorSynchronization"));
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			((TransactionSynchronization) synchs.get(1)).beforeCompletion();
			((TransactionSynchronization) synchs.get(1)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}

		sfControl.verify();
		sessionControl.verify();
	}

	public void testClobStringTypeWithFlushOnCommit() throws Exception {
		lobHandler.getClobAsString(rs, "column");
		lobHandlerControl.setReturnValue("content");
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringType type = new ClobStringType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.CLOB, type.sqlTypes()[0]);
		assertEquals(String.class, type.returnedClass());
		assertTrue(type.equals("content", "content"));
		assertEquals("content", type.deepCopy("content"));
		assertFalse(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, "content", 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testClobStringTypeWithJtaSynchronization() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		lobHandler.getClobAsString(rs, "column");
		lobHandlerControl.setReturnValue("content");
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringType type = new ClobStringType(lobHandler, tm);
		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		tmControl.verify();
	}

	public void testClobStringTypeWithJtaSynchronizationAndRollback() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		lobHandler.getClobAsString(rs, "column");
		lobHandlerControl.setReturnValue("content");
		lobCreator.setClobAsString(ps, 1, "content");
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		ClobStringType type = new ClobStringType(lobHandler, tm);
		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		tmControl.verify();
	}

	public void testBlobStringType() throws Exception {
		String content = "content";
		byte[] contentBytes = content.getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(contentBytes);
		lobCreator.setBlobAsBytes(ps, 1, contentBytes);
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobStringType type = new BlobStringType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(String.class, type.returnedClass());
		assertTrue(type.equals("content", "content"));
		assertEquals("content", type.deepCopy("content"));
		assertFalse(type.isMutable());

		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, content, 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobStringTypeWithNull() throws Exception {
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(null);
		lobCreator.setBlobAsBytes(ps, 1, null);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobStringType type = new BlobStringType(lobHandler, null);
		assertEquals(null, type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, null, 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobStringTypeWithJtaSynchronization() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		String content = "content";
		byte[] contentBytes = content.getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(contentBytes);
		lobCreator.setBlobAsBytes(ps, 1, contentBytes);
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobStringType type = new BlobStringType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		tmControl.verify();
	}

	public void testBlobStringTypeWithJtaSynchronizationAndRollback() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		String content = "content";
		byte[] contentBytes = content.getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(contentBytes);
		lobCreator.setBlobAsBytes(ps, 1, contentBytes);
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobStringType type = new BlobStringType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		tmControl.verify();
	}

	public void testBlobByteArrayType() throws Exception {
		byte[] content = "content".getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(content);
		lobCreator.setBlobAsBytes(ps, 1, content);
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobByteArrayType type = new BlobByteArrayType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(byte[].class, type.returnedClass());
		assertTrue(type.equals(new byte[] {(byte) 255}, new byte[] {(byte) 255}));
		assertTrue(Arrays.equals(new byte[] {(byte) 255}, (byte[]) type.deepCopy(new byte[] {(byte) 255})));
		assertTrue(type.isMutable());

		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, content, 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
			((TransactionSynchronization) synchs.get(0)).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobByteArrayTypeWithJtaSynchronization() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		byte[] content = "content".getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(content);
		lobCreator.setBlobAsBytes(ps, 1, content);
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobByteArrayType type = new BlobByteArrayType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		tmControl.verify();
	}

	public void testBlobByteArrayTypeWithJtaSynchronizationAndRollback() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		byte[] content = "content".getBytes();
		lobHandler.getBlobAsBytes(rs, "column");
		lobHandlerControl.setReturnValue(content);
		lobCreator.setBlobAsBytes(ps, 1, content);
		lobCreatorControl.setVoidCallable(1);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobByteArrayType type = new BlobByteArrayType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		tmControl.verify();
	}

	public void testBlobSerializableType() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		lobHandler.getBlobAsBinaryStream(rs, "column");
		lobHandlerControl.setReturnValue(new ByteArrayInputStream(baos.toByteArray()));
		lobCreator.setBlobAsBytes(ps, 1, baos.toByteArray());
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableType type = new BlobSerializableType(lobHandler, null);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(Serializable.class, type.returnedClass());
		assertTrue(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, "content", 1);
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
		lobHandler.getBlobAsBinaryStream(rs, "column");
		lobHandlerControl.setReturnValue(null);
		lobCreator.setBlobAsBytes(ps, 1, null);

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableType type = new BlobSerializableType(lobHandler, null);
		assertEquals(null, type.nullSafeGet(rs, new String[] {"column"}, null));
		TransactionSynchronizationManager.initSynchronization();
		try {
			type.nullSafeSet(ps, null, 1);
			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			((TransactionSynchronization) synchs.get(0)).beforeCompletion();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	public void testBlobSerializableTypeWithJtaSynchronization() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		lobHandler.getBlobAsBinaryStream(rs, "column");
		lobHandlerControl.setReturnValue(new ByteArrayInputStream(baos.toByteArray()));
		lobCreator.setBlobAsBytes(ps, 1, baos.toByteArray());
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableType type = new BlobSerializableType(lobHandler, tm);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(Serializable.class, type.returnedClass());
		assertTrue(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		tmControl.verify();
	}

	public void testBlobSerializableTypeWithJtaSynchronizationAndRollback() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 1);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		lobHandler.getBlobAsBinaryStream(rs, "column");
		lobHandlerControl.setReturnValue(new ByteArrayInputStream(baos.toByteArray()));
		lobCreator.setBlobAsBytes(ps, 1, baos.toByteArray());
		lobCreatorControl.setMatcher(new ArrayMatcher());

		lobHandlerControl.replay();
		lobCreatorControl.replay();

		BlobSerializableType type = new BlobSerializableType(lobHandler, tm);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(Serializable.class, type.returnedClass());
		assertTrue(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		tmControl.replay();
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		tmControl.verify();
	}

	public void testHbm2JavaStyleInitialization() throws Exception {
		rsControl.reset();
		psControl.reset();
		lobHandlerControl.reset();
		lobCreatorControl.reset();

		ClobStringType cst = null;
		BlobByteArrayType bbat = null;
		BlobSerializableType bst = null;
		try {
			cst = new ClobStringType();
			bbat = new BlobByteArrayType();
			bst = new BlobSerializableType();
		}
		catch (Exception ex) {
			fail("Should not have thrown exception on initialization");
		}

		try {
			cst.nullSafeGet(rs, new String[] {"column"}, null);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		try {
			bbat.nullSafeGet(rs, new String[] {"column"}, null);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		try {
			bst.nullSafeGet(rs, new String[] {"column"}, null);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
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
