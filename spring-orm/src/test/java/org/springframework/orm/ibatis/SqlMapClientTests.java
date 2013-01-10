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

package org.springframework.orm.ibatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Phillip Webb
 * @since 09.10.2004
 */
public class SqlMapClientTests {

	@Test
	public void testSqlMapClientFactoryBeanWithoutConfig() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		// explicitly set to null, don't know why ;-)
		factory.setConfigLocation(null);
		try {
			factory.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testSqlMapClientTemplate() throws SQLException {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		final SqlMapSession session = mock(SqlMapSession.class);
		SqlMapClient client = mock(SqlMapClient.class);

		given(ds.getConnection()).willReturn(con);
		given(client.openSession()).willReturn(session);

		SqlMapClientTemplate template = new SqlMapClientTemplate();
		template.setDataSource(ds);
		template.setSqlMapClient(client);
		template.afterPropertiesSet();
		Object result = template.execute(new SqlMapClientCallback() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) {
				assertTrue(executor == session);
				return "done";
			}
		});
		assertEquals("done", result);

		verify(con).close();
		verify(session).setUserConnection(con);
		verify(session).close();
	}

	@Test
	public void testSqlMapClientTemplateWithNestedSqlMapSession() throws SQLException {
		DataSource ds = mock(DataSource.class);
		final Connection con = mock(Connection.class);
		final SqlMapSession session = mock(SqlMapSession.class);
		SqlMapClient client = mock(SqlMapClient.class);

		given(client.openSession()).willReturn(session);
		given(session.getCurrentConnection()).willReturn(con);

		SqlMapClientTemplate template = new SqlMapClientTemplate();
		template.setDataSource(ds);
		template.setSqlMapClient(client);
		template.afterPropertiesSet();
		Object result = template.execute(new SqlMapClientCallback() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) {
				assertTrue(executor == session);
				return "done";
			}
		});
		assertEquals("done", result);
	}

	@Test
	public void testQueryForObjectOnSqlMapSession() throws SQLException {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		SqlMapClient client = mock(SqlMapClient.class);
		SqlMapSession session = mock(SqlMapSession.class);

		given(ds.getConnection()).willReturn(con);
		given(client.getDataSource()).willReturn(ds);
		given(client.openSession()).willReturn(session);
		given(session.queryForObject("myStatement", "myParameter")).willReturn("myResult");

		SqlMapClientTemplate template = new SqlMapClientTemplate();
		template.setSqlMapClient(client);
		template.afterPropertiesSet();
		assertEquals("myResult", template.queryForObject("myStatement", "myParameter"));

		verify(con).close();
		verify(session).setUserConnection(con);
		verify(session).close();
	}

	@Test
	public void testQueryForObject() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForObject("myStatement", null)).willReturn("myResult");
		assertEquals("myResult", template.queryForObject("myStatement"));
	}

	@Test
	public void testQueryForObjectWithParameter() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForObject("myStatement", "myParameter")).willReturn("myResult");
		assertEquals("myResult", template.queryForObject("myStatement", "myParameter"));
	}

	@Test
	public void testQueryForObjectWithParameterAndResultObject() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForObject("myStatement", "myParameter",
				"myResult")).willReturn("myResult");
		assertEquals("myResult", template.queryForObject("myStatement", "myParameter", "myResult"));
	}

	@Test
	public void testQueryForList() throws SQLException {
		List result = new ArrayList();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForList("myStatement", null)).willReturn(result);
		assertEquals(result, template.queryForList("myStatement"));
	}

	@Test
	public void testQueryForListWithParameter() throws SQLException {
		List result = new ArrayList();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForList("myStatement", "myParameter")).willReturn(result);
		assertEquals(result, template.queryForList("myStatement", "myParameter"));
	}

	@Test
	public void testQueryForListWithResultSize() throws SQLException {
		List result = new ArrayList();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForList("myStatement", null, 10, 20)).willReturn(result);
		assertEquals(result, template.queryForList("myStatement", 10, 20));
	}

	@Test
	public void testQueryForListParameterAndWithResultSize() throws SQLException {
		List result = new ArrayList();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForList("myStatement", "myParameter", 10, 20)).willReturn(result);
		assertEquals(result, template.queryForList("myStatement", "myParameter", 10, 20));
	}

	@Test
	public void testQueryWithRowHandler() throws SQLException {
		RowHandler rowHandler = new TestRowHandler();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		template.queryWithRowHandler("myStatement", rowHandler);
		verify(template.executor).queryWithRowHandler("myStatement", null, rowHandler);
	}

	@Test
	public void testQueryWithRowHandlerWithParameter() throws SQLException {
		RowHandler rowHandler = new TestRowHandler();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		template.queryWithRowHandler("myStatement", "myParameter", rowHandler);
		verify(template.executor).queryWithRowHandler("myStatement", "myParameter", rowHandler);
	}

	@Test
	public void testQueryForMap() throws SQLException {
		Map result = new HashMap();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForMap("myStatement", "myParameter", "myKey")).willReturn(result);
		assertEquals(result, template.queryForMap("myStatement", "myParameter", "myKey"));
	}

	@Test
	public void testQueryForMapWithValueProperty() throws SQLException {
		Map result = new HashMap();
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.queryForMap("myStatement", "myParameter", "myKey",
				"myValue")).willReturn(result);
		assertEquals(result, template.queryForMap("myStatement", "myParameter", "myKey", "myValue"));
	}

	@Test
	public void testInsert() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.insert("myStatement", null)).willReturn("myResult");
		assertEquals("myResult", template.insert("myStatement"));
	}

	@Test
	public void testInsertWithParameter() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.insert("myStatement", "myParameter")).willReturn("myResult");
		assertEquals("myResult", template.insert("myStatement", "myParameter"));
	}

	@Test
	public void testUpdate() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.update("myStatement", null)).willReturn(10);
		assertEquals(10, template.update("myStatement"));
	}

	@Test
	public void testUpdateWithParameter() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.update("myStatement", "myParameter")).willReturn(10);
		assertEquals(10, template.update("myStatement", "myParameter"));
	}

	@Test
	public void testUpdateWithRequiredRowsAffected() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.update("myStatement", "myParameter")).willReturn(10);
		template.update("myStatement", "myParameter", 10);
		verify(template.executor).update("myStatement", "myParameter");
	}

	@Test
	public void testUpdateWithRequiredRowsAffectedAndInvalidRowCount() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.update("myStatement", "myParameter")).willReturn(20);
		try {
			template.update("myStatement", "myParameter", 10);
			fail("Should have thrown JdbcUpdateAffectedIncorrectNumberOfRowsException");
		}
		catch (JdbcUpdateAffectedIncorrectNumberOfRowsException ex) {
			// expected
			assertEquals(10, ex.getExpectedRowsAffected());
			assertEquals(20, ex.getActualRowsAffected());
		}
	}

	@Test
	public void testDelete() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.delete("myStatement", null)).willReturn(10);
		assertEquals(10, template.delete("myStatement"));
	}

	@Test
	public void testDeleteWithParameter() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.delete("myStatement", "myParameter")).willReturn(10);
		assertEquals(10, template.delete("myStatement", "myParameter"));
	}

	@Test
	public void testDeleteWithRequiredRowsAffected() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.delete("myStatement", "myParameter")).willReturn(10);
		template.delete("myStatement", "myParameter", 10);
		verify(template.executor).delete("myStatement", "myParameter");
	}

	@Test
	public void testDeleteWithRequiredRowsAffectedAndInvalidRowCount() throws SQLException {
		TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
		given(template.executor.delete("myStatement", "myParameter")).willReturn(20);
		try {
			template.delete("myStatement", "myParameter", 10);
			fail("Should have thrown JdbcUpdateAffectedIncorrectNumberOfRowsException");
		}
		catch (JdbcUpdateAffectedIncorrectNumberOfRowsException ex) {
			// expected
			assertEquals(10, ex.getExpectedRowsAffected());
			assertEquals(20, ex.getActualRowsAffected());
		}
	}

	@Test
	public void testSqlMapClientDaoSupport() throws Exception {
		DataSource ds = mock(DataSource.class);
		SqlMapClientDaoSupport testDao = new SqlMapClientDaoSupport() {
		};
		testDao.setDataSource(ds);
		assertEquals(ds, testDao.getDataSource());

		SqlMapClient client = mock(SqlMapClient.class);

		testDao.setSqlMapClient(client);
		assertEquals(client, testDao.getSqlMapClient());

		SqlMapClientTemplate template = new SqlMapClientTemplate();
		template.setDataSource(ds);
		template.setSqlMapClient(client);
		testDao.setSqlMapClientTemplate(template);
		assertEquals(template, testDao.getSqlMapClientTemplate());

		testDao.afterPropertiesSet();
	}


	private static class TestSqlMapClientTemplate extends SqlMapClientTemplate {

		public SqlMapExecutor executor = mock(SqlMapExecutor.class);

		@Override
		public Object execute(SqlMapClientCallback action) throws DataAccessException {
			try {
				return action.doInSqlMapClient(executor);
			}
			catch (SQLException ex) {
				throw getExceptionTranslator().translate("SqlMapClient operation", null, ex);
			}
		}
	}


	private static class TestRowHandler implements RowHandler {

		@Override
		public void handleRow(Object row) {
		}
	}

}
