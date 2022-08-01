/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.datasource.lookup;


import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;


/**
 * Tests for {@link AbstractRoutingDataSource}.
 *
 * @author Kazuki Shimizu
 */
class AbstractRoutingDataSourceTests {

	@Test
	void setTargetDataSources() {
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
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds1);
		lookupKey.set("ds2");
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds2);
	}

	@Test
	void targetDataSourcesIsNull() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};
		assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
				.withMessage("Property 'targetDataSources' is required");
	}

	@Test
	void dataSourceIsUnSupportedType() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};
		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put("ds1", 1);
		routingDataSource.setTargetDataSources(targetDataSources);
		assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
				.withMessage("Illegal data source value - only [javax.sql.DataSource] and String supported: 1");
	}


	@Test
	void setDefaultTargetDataSource() {
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
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
	}

	@Test
	void setDefaultTargetDataSourceFallbackIsFalse() {
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
		lookupKey.set("foo");
		assertThatIllegalStateException().isThrownBy(routingDataSource::determineTargetDataSource)
				.withMessage("Cannot determine target DataSource for lookup key [foo]");
	}

	@Test
	void setDefaultTargetDataSourceLookupKeyIsNullWhenFallbackIsFalse() {
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
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
	}

	@Test
	public void notInitialized() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};
		assertThatIllegalArgumentException().isThrownBy(routingDataSource::determineTargetDataSource)
				.withMessage("DataSource router not initialized");
	}

}
