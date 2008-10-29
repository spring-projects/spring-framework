/*
 * Copyright 2002-2006 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 */
public final class MapDataSourceLookupTests extends TestCase {

	private static final String DATA_SOURCE_NAME = "dataSource";


	public void testGetDataSourcesReturnsUnmodifiableMap() throws Exception {
		new AssertThrows(UnsupportedOperationException.class, "The Map returned from getDataSources() *must* be unmodifiable") {
			public void test() throws Exception {
				MapDataSourceLookup lookup = new MapDataSourceLookup(new HashMap());
				Map dataSources = lookup.getDataSources();
				dataSources.put("", "");
			}
		}.runTest();
	}

	public void testLookupSunnyDay() throws Exception {
		Map dataSources = new HashMap();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertNotNull("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is", dataSource);
		assertSame(expectedDataSource, dataSource);
	}

	public void testSettingDataSourceMapToNullIsAnIdempotentOperation() throws Exception {
		Map dataSources = new HashMap();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		lookup.setDataSources(null); // must be idempotent (i.e. the following lookup must still work);
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertNotNull("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is", dataSource);
		assertSame(expectedDataSource, dataSource);
	}

	public void testAddingDataSourcePermitsOverride() throws Exception {
		Map dataSources = new HashMap();
		StubDataSource overridenDataSource = new StubDataSource();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, overridenDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		lookup.addDataSource(DATA_SOURCE_NAME, expectedDataSource); // must override existing entry
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertNotNull("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is", dataSource);
		assertSame(expectedDataSource, dataSource);
	}

	public void testGetDataSourceWhereSuppliedMapHasNonDataSourceTypeUnderSpecifiedKey() throws Exception {
		new AssertThrows(DataSourceLookupFailureException.class) {
			public void test() throws Exception {
				Map dataSources = new HashMap();
				dataSources.put(DATA_SOURCE_NAME, new Object());
				MapDataSourceLookup lookup = new MapDataSourceLookup();
				lookup.setDataSources(dataSources);
				lookup.getDataSource(DATA_SOURCE_NAME);
			}
		}.runTest();
	}

	public void testGetDataSourceWhereSuppliedMapHasNoEntryForSpecifiedKey() throws Exception {
		new AssertThrows(DataSourceLookupFailureException.class) {
			public void test() throws Exception {
				MapDataSourceLookup lookup = new MapDataSourceLookup();
				lookup.getDataSource(DATA_SOURCE_NAME);
			}
		}.runTest();
	}

}
