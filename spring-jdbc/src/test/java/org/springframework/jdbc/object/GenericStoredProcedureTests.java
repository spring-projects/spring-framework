/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.TestDataSourceWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Thomas Risberg
 */
public class GenericStoredProcedureTests {

	@Test
	public void testAddInvoices() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("org/springframework/jdbc/object/GenericStoredProcedureTests-context.xml"));
		Connection connection = mock();
		DataSource dataSource = mock();
		given(dataSource.getConnection()).willReturn(connection);
		CallableStatement callableStatement = mock();
		TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) bf.getBean("dataSource");
		testDataSource.setTarget(dataSource);

		given(callableStatement.execute()).willReturn(false);
		given(callableStatement.getUpdateCount()).willReturn(-1);
		given(callableStatement.getObject(3)).willReturn(4);

		given(connection.prepareCall("{call " + "add_invoice" + "(?, ?, ?)}")).willReturn(callableStatement);

		StoredProcedure adder = (StoredProcedure) bf.getBean("genericProcedure");
		Map<String, Object> in = Map.of("amount", 1106, "custid", 3);
		Map<String, Object> out = adder.execute(in);
		assertThat(out.get("newid")).isEqualTo(4);

		verify(callableStatement).setObject(1, 1106, Types.INTEGER);
		verify(callableStatement).setObject(2, 3, Types.INTEGER);
		verify(callableStatement).registerOutParameter(3, Types.INTEGER);
		verify(callableStatement).close();
	}

}
