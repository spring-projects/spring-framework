/*
 * Copyright 2002-2024 the original author or authors.
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
		ThreadLocal<String> lookupKey = new ThreadLocal<>();
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
		routingDataSource.setTargetDataSources(Map.of("ds1", ds1, "ds2", "dataSource2"));
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
		routingDataSource.setTargetDataSources(Map.of("ds1", 1));
		assertThatIllegalArgumentException().isThrownBy(routingDataSource::afterPropertiesSet)
				.withMessage("Illegal data source value - only [javax.sql.DataSource] and String supported: 1");
	}


	@Test
	void setDefaultTargetDataSource() {
		ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();
		routingDataSource.setTargetDataSources(Map.of());
		routingDataSource.setDefaultTargetDataSource(ds);
		routingDataSource.afterPropertiesSet();
		lookupKey.set("foo");
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
	}

	@Test
	void setDefaultTargetDataSourceFallbackIsFalse() {
		ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();
		routingDataSource.setTargetDataSources(Map.of());
		routingDataSource.setDefaultTargetDataSource(ds);
		routingDataSource.setLenientFallback(false);
		routingDataSource.afterPropertiesSet();
		lookupKey.set("foo");
		assertThatIllegalStateException().isThrownBy(routingDataSource::determineTargetDataSource)
				.withMessage("Cannot determine target DataSource for lookup key [foo]");
	}

	@Test
	void setDefaultTargetDataSourceLookupKeyIsNullWhenFallbackIsFalse() {
		ThreadLocal<String> lookupKey = new ThreadLocal<>();
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return lookupKey.get();
			}
		};
		DataSource ds = new StubDataSource();
		routingDataSource.setTargetDataSources(Map.of());
		routingDataSource.setDefaultTargetDataSource(ds);
		routingDataSource.setLenientFallback(false);
		routingDataSource.afterPropertiesSet();
		lookupKey.remove();
		assertThat(routingDataSource.determineTargetDataSource()).isSameAs(ds);
	}

	@Test
	void initializeSynchronizesTargetDataSourcesToResolvedDataSources() {
		AbstractRoutingDataSource routingDataSource = new AbstractRoutingDataSource() {
			@Override
			protected Object determineCurrentLookupKey() {
				return null;
			}
		};

		DataSource ds1 = new StubDataSource();
		DataSource ds2 = new StubDataSource();

		routingDataSource.setTargetDataSources(Map.of("ds1", ds1, "ds2", ds2));
		routingDataSource.initialize();

		Map<Object, DataSource> resolvedDataSources = routingDataSource.getResolvedDataSources();
		assertThat(resolvedDataSources).hasSize(2);
		assertThat(resolvedDataSources.get("ds1")).isSameAs(ds1);
		assertThat(resolvedDataSources.get("ds2")).isSameAs(ds2);
	}

	@Test
	void notInitialized() {
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
