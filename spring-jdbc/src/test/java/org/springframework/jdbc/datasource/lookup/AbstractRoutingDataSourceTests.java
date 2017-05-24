/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AbstractRoutingDataSource}.
 * @author Kazuki Shimizu
 * @since 4.3.7
 */
public class AbstractRoutingDataSourceTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void setTargetDataSources() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds1 = new StubDataSource();
		DataSource ds2 = new StubDataSource();

		MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
		dataSourceLookup.addDataSource("dataSource2", ds2);
		routingDataSource.setDataSourceLookup(dataSourceLookup);
		
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put("ds1", ds1);
		targetDataSources.put("ds2", "dataSource2");
		routingDataSource.setTargetDataSources(targetDataSources);

		routingDataSource.afterPropertiesSet();

		lookupKey.set("ds1");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds1));
		
		lookupKey.set("ds2");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds2));
	}

	@Test
	public void targetDataSourcesIsNull() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Property 'targetDataSources' is required");

		routingDataSource.afterPropertiesSet();
	}

	@Test
	public void dataSourceIsUnSupportedType() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put("ds1", 1);
		routingDataSource.setTargetDataSources(targetDataSources);

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Illegal data source value - only [javax.sql.DataSource] and String supported: 1");

		routingDataSource.afterPropertiesSet();
	}



	@Test
	public void setDefaultTargetDataSource() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();

		routingDataSource.setTargetDataSources(new HashMap<>());
		routingDataSource.setDefaultTargetDataSource(ds);

		routingDataSource.afterPropertiesSet();

		lookupKey.set("foo");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds));
	}

	@Test
	public void setDefaultTargetDataSourceFallbackIsFalse() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();

		routingDataSource.setTargetDataSources(new HashMap<>());
		routingDataSource.setDefaultTargetDataSource(ds);
		routingDataSource.setLenientFallback(false);

		routingDataSource.afterPropertiesSet();

		exception.expect(IllegalStateException.class);
		exception.expectMessage("Cannot determine target DataSource for lookup key [foo]");

		lookupKey.set("foo");
		routingDataSource.determineTargetDataSource();
	}

	@Test
	public void setDefaultTargetDataSourceLookupKeyIsNullWhenFallbackIsFalse() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();

		routingDataSource.setTargetDataSources(new HashMap<>());
		routingDataSource.setDefaultTargetDataSource(ds);
		routingDataSource.setLenientFallback(false);

		routingDataSource.afterPropertiesSet();

		lookupKey.set(null);
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds));
	}

	@Test // SPR-15253
	public void addTargetDataSource() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds1 = new StubDataSource();
		DataSource ds2 = new StubDataSource();
		routingDataSource.addTargetDataSource("ds1", ds1);
		routingDataSource.addTargetDataSource("ds2", ds2);
		
		routingDataSource.afterPropertiesSet();

		lookupKey.set("ds1");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds1));

		lookupKey.set("ds2");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds2));
	}
	
	@Test // SPR-15253
	public void addTargetDataSourceName() {
		final ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds1 = new StubDataSource();
		DataSource ds2 = new StubDataSource();
		MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
		dataSourceLookup.addDataSource("dataSource1", ds1);
		dataSourceLookup.addDataSource("dataSource2", ds2);
		routingDataSource.setDataSourceLookup(dataSourceLookup);

		routingDataSource.addTargetDataSourceName("ds1", "dataSource1");
		routingDataSource.addTargetDataSourceName("ds2", "dataSource2");
		
		routingDataSource.afterPropertiesSet();
		
		lookupKey.set("ds1");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds1));
		
		lookupKey.set("ds2");
		assertThat(routingDataSource.determineTargetDataSource(), sameInstance(ds2));
	}

	@Test
	public void notInitialized() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("DataSource router not initialized");

		routingDataSource.determineTargetDataSource();
	}

}
