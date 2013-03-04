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

package org.springframework.orm.ibatis.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 27.02.2005
 */
public class LobTypeHandlerTests {

	private ResultSet rs = mock(ResultSet.class);
	private PreparedStatement ps = mock(PreparedStatement.class);

	private LobHandler lobHandler = mock(LobHandler.class);
	private LobCreator lobCreator = mock(LobCreator.class);

	@Before
	public void setUp() throws Exception {
		given(rs.findColumn("column")).willReturn(1);
		given(lobHandler.getLobCreator()).willReturn(lobCreator);
	}

	@After
	public void tearDown() throws Exception {
		verify(lobCreator).close();
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testClobStringTypeHandler() throws Exception {
		given(lobHandler.getClobAsString(rs, 1)).willReturn("content");

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
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testClobStringTypeWithSynchronizedConnection() throws Exception {
		DataSource dsTarget = new DriverManagerDataSource();
		DataSource ds = new LazyConnectionDataSourceProxy(dsTarget);

		given(lobHandler.getClobAsString(rs, 1)).willReturn("content");

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
		verify(lobCreator).setClobAsString(ps, 1, "content");
	}

	@Test
	public void testBlobByteArrayType() throws Exception {
		byte[] content = "content".getBytes();
		given(lobHandler.getBlobAsBytes(rs, 1)).willReturn(content);

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
		verify(lobCreator).setBlobAsBytes(ps, 1, content);
	}

	@Test
	public void testBlobSerializableType() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject("content");
		oos.close();

		given(lobHandler.getBlobAsBinaryStream(rs, 1)).willAnswer(new Answer<InputStream>() {
			@Override
			public InputStream answer(InvocationOnMock invocation)
					throws Throwable {
				return new ByteArrayInputStream(baos.toByteArray());
			}
		});

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
		verify(lobCreator).setBlobAsBytes(ps, 1, baos.toByteArray());
	}

	@Test
	public void testBlobSerializableTypeWithNull() throws Exception {
		given(lobHandler.getBlobAsBinaryStream(rs, 1)).willReturn(null);

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
		verify(lobCreator).setBlobAsBytes(ps, 1, null);
	}
}
