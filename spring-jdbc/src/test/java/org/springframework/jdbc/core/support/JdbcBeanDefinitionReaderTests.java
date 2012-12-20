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

package org.springframework.jdbc.core.support;

import java.sql.ResultSet;
import java.sql.Statement;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Rod Johnson
 */
public class JdbcBeanDefinitionReaderTests extends AbstractJdbcTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();


	public void testValid() throws Exception {
		String sql = "SELECT NAME AS NAME, PROPERTY AS PROPERTY, VALUE AS VALUE FROM T";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		ctrlResultSet.expectAndReturn(mockResultSet.next(), true, 2);
		ctrlResultSet.expectAndReturn(mockResultSet.next(), false);

		// first row
		ctrlResultSet.expectAndReturn(mockResultSet.getString(1), "one");
		ctrlResultSet.expectAndReturn(mockResultSet.getString(2), "(class)");
		ctrlResultSet.expectAndReturn(mockResultSet.getString(3), "org.springframework.beans.TestBean");

		// second row
		ctrlResultSet.expectAndReturn(mockResultSet.getString(1), "one");
		ctrlResultSet.expectAndReturn(mockResultSet.getString(2), "age");
		ctrlResultSet.expectAndReturn(mockResultSet.getString(3), "53");

		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		MockControl ctrlStatement = MockControl.createControl(Statement.class);
		Statement mockStatement = (Statement) ctrlStatement.getMock();
		ctrlStatement.expectAndReturn(mockStatement.executeQuery(sql), mockResultSet);
		if (debugEnabled) {
			ctrlStatement.expectAndReturn(mockStatement.getWarnings(), null);
		}
		mockStatement.close();
		ctrlStatement.setVoidCallable();

		mockConnection.createStatement();
		ctrlConnection.setReturnValue(mockStatement);

		ctrlResultSet.replay();
		ctrlStatement.replay();
		replay();

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		JdbcBeanDefinitionReader reader = new JdbcBeanDefinitionReader(bf);
		reader.setDataSource(mockDataSource);
		reader.loadBeanDefinitions(sql);
		assertEquals("Incorrect number of bean definitions", 1, bf.getBeanDefinitionCount());
		TestBean tb = (TestBean) bf.getBean("one");
		assertEquals("Age in TestBean was wrong.", 53, tb.getAge());

		ctrlResultSet.verify();
		ctrlStatement.verify();
	}

}
