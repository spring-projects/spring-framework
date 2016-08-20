/*
 * Copyright 2002-2016 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.TestDataSourceWrapper;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thomas Risberg
 */
public class GenericStoredProcedureTests {

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
		given(callableStatement.getObject(3)).willReturn(4);

		given(connection.prepareCall("{call " + "add_invoice" + "(?, ?, ?)}")).willReturn(callableStatement);

		StoredProcedure adder = (StoredProcedure) bf.getBean("genericProcedure");
		Map<String, Object> in = new HashMap<>(2);
		in.put("amount", 1106);
		in.put("custid", 3);
		Map<String, Object> out = adder.execute(in);
		Integer id = (Integer) out.get("newid");
		assertEquals(4, id.intValue());

		verify(callableStatement).setObject(1, 1106, Types.INTEGER);
		verify(callableStatement).setObject(2, 3, Types.INTEGER);
		verify(callableStatement).registerOutParameter(3, Types.INTEGER);
		verify(callableStatement).close();
	}

}
