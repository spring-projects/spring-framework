/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.lookup;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class MapDataSourceLookupTests {

	private static final String DATA_SOURCE_NAME = "dataSource";


	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void getDataSourcesReturnsUnmodifiableMap() {
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		Map dataSources = lookup.getDataSources();

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				dataSources.put("", ""));
	}

	@Test
	void lookupSunnyDay() {
		Map<String, DataSource> dataSources = new HashMap<>();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
		assertThat(dataSource).isSameAs(expectedDataSource);
	}

	@Test
	void setDataSourcesIsAnIdempotentOperation() {
		Map<String, DataSource> dataSources = new HashMap<>();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, expectedDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		lookup.setDataSources(null); // must be idempotent (i.e. the following lookup must still work);
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
		assertThat(dataSource).isSameAs(expectedDataSource);
	}

	@Test
	void addingDataSourcePermitsOverride() {
		Map<String, DataSource> dataSources = new HashMap<>();
		StubDataSource overriddenDataSource = new StubDataSource();
		StubDataSource expectedDataSource = new StubDataSource();
		dataSources.put(DATA_SOURCE_NAME, overriddenDataSource);
		MapDataSourceLookup lookup = new MapDataSourceLookup();
		lookup.setDataSources(dataSources);
		lookup.addDataSource(DATA_SOURCE_NAME, expectedDataSource); // must override existing entry
		DataSource dataSource = lookup.getDataSource(DATA_SOURCE_NAME);
		assertThat(dataSource).as("A DataSourceLookup implementation must *never* return null from getDataSource(): this one obviously (and incorrectly) is").isNotNull();
		assertThat(dataSource).isSameAs(expectedDataSource);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void getDataSourceWhereSuppliedMapHasNonDataSourceTypeUnderSpecifiedKey() {
		Map dataSources = new HashMap();
		dataSources.put(DATA_SOURCE_NAME, new Object());
		MapDataSourceLookup lookup = new MapDataSourceLookup(dataSources);

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
				lookup.getDataSource(DATA_SOURCE_NAME));
	}

	@Test
	void getDataSourceWhereSuppliedMapHasNoEntryForSpecifiedKey() {
		MapDataSourceLookup lookup = new MapDataSourceLookup();

		assertThatExceptionOfType(DataSourceLookupFailureException.class).isThrownBy(() ->
				lookup.getDataSource(DATA_SOURCE_NAME));
	}

}
