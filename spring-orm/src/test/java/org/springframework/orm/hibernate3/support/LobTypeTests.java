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

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.tests.transaction.MockJtaTransaction;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 05.03.2005
 */
public class LobTypeTests {

	private ResultSet rs = mock(ResultSet.class);
	private PreparedStatement ps = mock(PreparedStatement.class);
	private LobHandler lobHandler = mock(LobHandler.class);
	private LobCreator lobCreator = mock(LobCreator.class);

	@Before
	public void setUp() throws SQLException {
		given(lobHandler.getLobCreator()).willReturn(lobCreator);
	}

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		verify(lobCreator).close();
	}

	@Test
	public void testClobStringType() throws Exception {
		given(lobHandler.getClobAsString(rs, "column")).willReturn("content");

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
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testClobStringTypeWithSynchronizedSession() throws Exception {
		SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(lobHandler.getClobAsString(rs, "column")).willReturn("content");

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

		verify(session).close();
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testClobStringTypeWithFlushOnCommit() throws Exception {
		given(lobHandler.getClobAsString(rs, "column")).willReturn("content");

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
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testClobStringTypeWithJtaSynchronization() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		given(lobHandler.getClobAsString(rs, "column")).willReturn("content");

		ClobStringType type = new ClobStringType(lobHandler, tm);
		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testClobStringTypeWithJtaSynchronizationAndRollback() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);
		given(lobHandler.getClobAsString(rs, "column")).willReturn("content");

		ClobStringType type = new ClobStringType(lobHandler, tm);
		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);

		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testBlobStringType() throws Exception {
		String content = "content";
		byte[] contentBytes = content.getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(contentBytes);

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
		verify(lobCreator).setBlobAsBytes(ps, 1, contentBytes);
	}

	@Test
	public void testBlobStringTypeWithNull() throws Exception {
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(null);

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

		verify(lobCreator).setBlobAsBytes(ps, 1, null);
	}

	@Test
	public void testBlobStringTypeWithJtaSynchronization() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		String content = "content";
		byte[] contentBytes = content.getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(contentBytes);

		BlobStringType type = new BlobStringType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);

		verify(lobCreator).setBlobAsBytes(ps, 1, contentBytes);
	}

	@Test
	public void testBlobStringTypeWithJtaSynchronizationAndRollback() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		String content = "content";
		byte[] contentBytes = content.getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(contentBytes);

		BlobStringType type = new BlobStringType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		verify(lobCreator).setBlobAsBytes(ps, 1, contentBytes);
	}

	@Test
	public void testBlobByteArrayType() throws Exception {
		byte[] content = "content".getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(content);

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
		verify(lobCreator).setBlobAsBytes(ps, 1, content);
	}

	@Test
	public void testBlobByteArrayTypeWithJtaSynchronization() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		byte[] content = "content".getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(content);

		BlobByteArrayType type = new BlobByteArrayType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		verify(lobCreator).setBlobAsBytes(ps, 1, content);
	}

	@Test
	public void testBlobByteArrayTypeWithJtaSynchronizationAndRollback() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		byte[] content = "content".getBytes();
		given(lobHandler.getBlobAsBytes(rs, "column")).willReturn(content);

		BlobByteArrayType type = new BlobByteArrayType(lobHandler, tm);
		assertEquals(content, type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, content, 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		verify(lobCreator).setBlobAsBytes(ps, 1, content);
	}

	@Test
	public void testBlobSerializableType() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		given(lobHandler.getBlobAsBinaryStream(rs, "column")).willReturn(new ByteArrayInputStream(baos.toByteArray()));

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
		verify(lobCreator).setBlobAsBytes(ps, 1, baos.toByteArray());
	}

	@Test
	public void testBlobSerializableTypeWithNull() throws Exception {
		given(lobHandler.getBlobAsBinaryStream(rs, "column")).willReturn(null);

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
		verify(lobCreator).setBlobAsBytes(ps, 1, null);
	}

	@Test
	public void testBlobSerializableTypeWithJtaSynchronization() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		given(lobHandler.getBlobAsBinaryStream(rs, "column")).willReturn(
				new ByteArrayInputStream(baos.toByteArray()));

		BlobSerializableType type = new BlobSerializableType(lobHandler, tm);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(Serializable.class, type.returnedClass());
		assertTrue(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.beforeCompletion();
		synch.afterCompletion(Status.STATUS_COMMITTED);
		verify(lobCreator).setBlobAsBytes(ps, 1, baos.toByteArray());
	}

	@Test
	public void testBlobSerializableTypeWithJtaSynchronizationAndRollback() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		given(lobHandler.getBlobAsBinaryStream(rs, "column")).willReturn(
				new ByteArrayInputStream(baos.toByteArray()));

		BlobSerializableType type = new BlobSerializableType(lobHandler, tm);
		assertEquals(1, type.sqlTypes().length);
		assertEquals(Types.BLOB, type.sqlTypes()[0]);
		assertEquals(Serializable.class, type.returnedClass());
		assertTrue(type.isMutable());

		assertEquals("content", type.nullSafeGet(rs, new String[] {"column"}, null));
		type.nullSafeSet(ps, "content", 1);
		Synchronization synch = transaction.getSynchronization();
		assertNotNull(synch);
		synch.afterCompletion(Status.STATUS_ROLLEDBACK);
		verify(lobCreator).setBlobAsBytes(ps, 1, baos.toByteArray());
	}

	@Test
	public void testHbm2JavaStyleInitialization() throws Exception {
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
		lobCreator.close();
	}
}
