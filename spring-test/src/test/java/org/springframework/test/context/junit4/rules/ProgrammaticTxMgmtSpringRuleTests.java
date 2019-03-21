/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import javax.sql.DataSource;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.programmatic.ProgrammaticTxMgmtTests;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This class is an extension of {@link ProgrammaticTxMgmtTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@ContextConfiguration
public class ProgrammaticTxMgmtSpringRuleTests extends ProgrammaticTxMgmtTests {

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	// All tests are in superclass.

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.generateUniqueName(true)//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql") //
			.build();
		}
	}

}
