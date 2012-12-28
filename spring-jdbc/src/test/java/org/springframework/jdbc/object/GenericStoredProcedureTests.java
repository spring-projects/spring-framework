/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;

import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.datasource.TestDataSourceWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Thomas Risberg
 */
@RunWith(JUnit4.class)
public class GenericStoredProcedureTests extends AbstractJdbcTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private CallableStatement mockCallable;

	private BeanFactory bf;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		mockCallable = createMock(CallableStatement.class);
		bf = new XmlBeanFactory(
				new ClassPathResource("org/springframework/jdbc/object/GenericStoredProcedureTests-context.xml"));
		TestDataSourceWrapper testDataSource = (TestDataSourceWrapper) bf.getBean("dataSource");
		testDataSource.setTarget(mockDataSource);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (shouldVerify()) {
			EasyMock.verify(mockCallable);
		}
	}

	@Override
	protected void replay() {
		super.replay();
		EasyMock.replay(mockCallable);
	}

	@Test
	public void testAddInvoices() throws Exception {

		mockCallable.setObject(1, new Integer(1106), Types.INTEGER);
		expectLastCall();
		mockCallable.setObject(2, new Integer(3), Types.INTEGER);
		expectLastCall();
		mockCallable.registerOutParameter(3, Types.INTEGER);
		expectLastCall();
		expect(mockCallable.execute()).andReturn(false);
		expect(mockCallable.getUpdateCount()).andReturn(-1);
		expect(mockCallable.getObject(3)).andReturn(new Integer(4));
		if (debugEnabled) {
			expect(mockCallable.getWarnings()).andReturn(null);
		}
		mockCallable.close();
		expectLastCall();

		mockConnection.prepareCall("{call " + "add_invoice" + "(?, ?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		testAddInvoice(1106, 3);
	}

	private void testAddInvoice(final int amount, final int custid)
		throws Exception {

		StoredProcedure adder = (StoredProcedure) bf.getBean("genericProcedure");
		Map<String, Object> in = new HashMap<String, Object>(2);
		in.put("amount", amount);
		in.put("custid", custid);
		Map out = adder.execute(in);
		Integer id = (Integer) out.get("newid");
		assertEquals(4, id.intValue());
	}

}