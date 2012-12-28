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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanFactoryDataSourceLookupTests {

	private static final String DATASOURCE_BEAN_NAME = "dataSource";


	@Test
	public void testLookupSunnyDay() {
		BeanFactory beanFactory = mock(BeanFactory.class);

		StubDataSource expectedDataSource = new StubDataSource();
		given(beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class)).willReturn(expectedDataSource);

		BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
		lookup.setBeanFactory(beanFactory);
		DataSource dataSource = lookup.getDataSource(DATASOURCE_BEAN_NAME);
		assertNotNull("A DataSourceLookup implementation must *never* return null from " +
				"getDataSource(): this one obviously (and incorrectly) is", dataSource);
		assertSame(expectedDataSource, dataSource);
	}

	@Test
	public void testLookupWhereBeanFactoryYieldsNonDataSourceType() throws Exception {
		final BeanFactory beanFactory = mock(BeanFactory.class);

		given(beanFactory.getBean(DATASOURCE_BEAN_NAME, DataSource.class)).willThrow(
				new BeanNotOfRequiredTypeException(DATASOURCE_BEAN_NAME,
						DataSource.class, String.class));

		try {
				BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup(beanFactory);
				lookup.getDataSource(DATASOURCE_BEAN_NAME);
				fail("should have thrown DataSourceLookupFailureException");
		} catch (DataSourceLookupFailureException ex) { /* expected */ }
	}

	@Test(expected=IllegalStateException.class)
	public void testLookupWhereBeanFactoryHasNotBeenSupplied() throws Exception {
		BeanFactoryDataSourceLookup lookup = new BeanFactoryDataSourceLookup();
		lookup.getDataSource(DATASOURCE_BEAN_NAME);
	}

}
