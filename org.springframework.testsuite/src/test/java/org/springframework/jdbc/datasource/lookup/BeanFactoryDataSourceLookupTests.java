/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BeanFactoryDataSourceLookupTests extends TestCase {

	private static final String DATASOURCE_BEAN_NAME = "dataSource";


	public void testLookupSunnyDay() throws Exception {
		MockControl mockBeanFactory = MockControl.createControl(BeanFactory.class);
		BeanFactory beanFactory = (BeanFactory) mockBeanFactory.getMock();

		beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class);
		StubDataSource expectedDataSource = new StubDataSource();
		mockBeanFactory.setReturnValue(expectedDataSource);

		mockBeanFactory.replay();

		BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
		lookup.setBeanFactory(beanFactory);
		DataSource dataSource = lookup.getDataSource(DATASOURCE_BEAN_NAME);
		assertNotNull("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is", dataSource);
		assertSame(expectedDataSource, dataSource);

		mockBeanFactory.verify();
	}

	public void testLookupWhereBeanFactoryYieldsNonDataSourceType() throws Exception {
		MockControl mockBeanFactory = MockControl.createControl(BeanFactory.class);
		final BeanFactory beanFactory = (BeanFactory) mockBeanFactory.getMock();

		beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class);
		mockBeanFactory.setThrowable(new BeanNotOfRequiredTypeException(DATASOURCE_BEAN_NAME, DataSource.class, String.class));

		mockBeanFactory.replay();

		new AssertThrows(DataSourceLookupFailureException.class) {
			public void test() throws Exception {
				BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup(beanFactory);
				lookup.getDataSource(DATASOURCE_BEAN_NAME);
			}
		}.runTest();

		mockBeanFactory.verify();
	}

	public void testLookupWhereBeanFactoryHasNotBeenSupplied() throws Exception {
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
				lookup.getDataSource(DATASOURCE_BEAN_NAME);
			}
		}.runTest();
	}

}
