/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.jdbc;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;

/**
 * Test suite to investigate claims raised in
 * <a href="https://github.com/spring-projects/spring-framework/issues/13491">gh-13491</a>.
 *
 * <h3>Work Around</h3>
 * <p>By using a SpEL expression to generate a random {@code database-name}
 * for the embedded database (see {@code datasource-config.xml}), we ensure
 * that each {@code ApplicationContext} that imports the common configuration
 * will create an embedded database with a unique name.
 *
 * <p>To reproduce the problem mentioned in gh-13491, delete the declaration
 * of the {@code database-name} attribute of the embedded database in
 * {@code datasource-config.xml} and run this <em>suite</em>.
 *
 * <h3>Solution</h3>
 * <p>As of Spring 4.2, a proper solution is possible thanks to gh-13491.
 * {@link TestClass2A} and {@link TestClass2B} both import
 * {@code datasource-config-with-auto-generated-db-name.xml} which makes
 * use of the new {@code generate-name} attribute of {@code <jdbc:embedded-database>}.
 *
 * @author Sam Brannen
 * @author Mickael Leduque
 */
class GeneratedDatabaseNamesTests {

	private static final String DATASOURCE_CONFIG_XML =
			"classpath:/org/springframework/test/context/jdbc/datasource-config.xml";

	private static final String DATASOURCE_CONFIG_WITH_AUTO_GENERATED_DB_NAME_XML =
			"classpath:/org/springframework/test/context/jdbc/datasource-config-with-auto-generated-db-name.xml";


	@Test
	void runTestsWithGeneratedDatabaseNames() {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClasses(TestClass1A.class, TestClass1B.class, TestClass2A.class, TestClass2B.class))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(4).succeeded(4).failed(0));
	}


	@ExtendWith(SpringExtension.class)
	abstract static class AbstractTestCase {

		@Resource
		DataSource dataSource;

		@Test
		void test() {
			assertThat(dataSource).isNotNull();
		}
	}

	@ContextConfiguration
	static class TestClass1A extends AbstractTestCase {

		@Configuration
		@ImportResource(DATASOURCE_CONFIG_XML)
		static class Config {
		}
	}

	@ContextConfiguration
	static class TestClass1B extends AbstractTestCase {

		@Configuration
		@ImportResource(DATASOURCE_CONFIG_XML)
		static class Config {
		}
	}

	/**
	 * @since 4.2
	 */
	@ContextConfiguration
	static class TestClass2A extends AbstractTestCase {

		@Configuration
		@ImportResource(DATASOURCE_CONFIG_WITH_AUTO_GENERATED_DB_NAME_XML)
		static class Config {
		}
	}

	/**
	 * @since 4.2
	 */
	@ContextConfiguration
	static class TestClass2B extends AbstractTestCase {

		@Configuration
		@ImportResource(DATASOURCE_CONFIG_WITH_AUTO_GENERATED_DB_NAME_XML)
		static class Config {
		}
	}

}
