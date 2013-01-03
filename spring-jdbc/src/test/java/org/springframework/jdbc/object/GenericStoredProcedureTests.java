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

package org.springframework.jdbc.object;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TestDataSourceWrapper;

/**
 * @author Thomas Risberg
 */
public class GenericStoredProcedureTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	@Test
	public void testAddInvoices() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("org/springframework/jdbc/object/GenericStoredProcedureTests-context.xml"));
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);
		CallableStatement callableStatement = mock(CallableStatement.class);
		TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) bf.getBean("dataSource");
		testDataSource.setTarget(dataSource);

		given(callableStatement.execute()).willReturn(false);
		given(callableStatement.getUpdateCount()).willReturn(-1);
		given(callableStatement.getObject(3)).willReturn(new Integer(4));

		given(connection.prepareCall("{call " + "add_invoice" + "(?, ?, ?)}")).willReturn(callableStatement);

		StoredProcedure adder = (StoredProcedure) bf.getBean("genericProcedure");
		Map<String, Object> in = new HashMap<String, Object>(2);
		in.put("amount", 1106);
		in.put("custid", 3);
		Map out = adder.execute(in);
		Integer id = (Integer) out.get("newid");
		assertEquals(4, id.intValue());

		verify(callableStatement).setObject(1, new Integer(1106), Types.INTEGER);
		verify(callableStatement).setObject(2, new Integer(3), Types.INTEGER);
		verify(callableStatement).registerOutParameter(3, Types.INTEGER);
		verify(callableStatement).close();
	}

}
