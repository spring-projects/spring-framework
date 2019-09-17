/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests proving that after the creation of new context, transaction aspects are cleared of
 * transaction managers created in previous context.
 *
 * @author Semyon Danilov
 */
class AnnotationTransactionAspectTest {

	private static final String TRANSACTION_MANAGER_KEY = "org.springframework.transaction.config.internalTransactionAspect";
	private static final String JAVAX_TRANSACTION_MANAGER_KEY = "org.springframework.transaction.config.internalJtaTransactionAspect";

	@Test
	void testAspectTxManagerCacheCleanInNewContext() {
		// stub txAttr to get txManager
		TransactionAttribute txAttr = attr();

		// create first context
		AnnotationConfigApplicationContext ctx1 = new AnnotationConfigApplicationContext();
		ctx1.register(Config.class, AspectJTxConfig.class);
		ctx1.refresh();

		// invoke transactional method thus initializing txManagerCache in aspect
		final JdbcFooRepository repository1 = ctx1.getBean(JdbcFooRepository.class);
		repository1.findAll();
		repository1.findAllJta();

		// get aspect bean (it's a real singleton, it's one per jvm)
		TransactionAspectSupport supportBean = ctx1.getBeansOfType(TransactionAspectSupport.class).get(TRANSACTION_MANAGER_KEY);
		TransactionAspectSupport supportBeanJavax = ctx1.getBeansOfType(TransactionAspectSupport.class).get(JAVAX_TRANSACTION_MANAGER_KEY);

		// get txManager
		final PlatformTransactionManager firstManager = supportBean.determineTransactionManager(txAttr);
		final PlatformTransactionManager firstManagerJavax = supportBeanJavax.determineTransactionManager(txAttr);

		// create second context, little different from first
		AnnotationConfigApplicationContext ctx2 = new AnnotationConfigApplicationContext();
		ctx2.register(Config.class, AspectJTxConfig.class, SomeOtherConfig.class);
		ctx2.refresh();

		// invoke transactional method thus initializing txManagerCache in aspect
		final JdbcFooRepository repository2 = ctx2.getBean(JdbcFooRepository.class);
		repository2.findAll();
		repository2.findAllJta();

		// get new txManager
		final PlatformTransactionManager secondManager = supportBean.determineTransactionManager(txAttr);
		final PlatformTransactionManager secondManagerJavax = supportBeanJavax.determineTransactionManager(txAttr);

		// check that txManager was NOT cached
		assertThat(firstManager).isNotEqualTo(secondManager);
		assertThat(firstManagerJavax).isNotEqualTo(secondManagerJavax);
	}


	@Test
	void testAspectTxManagerFieldCleanInNewContext() {
		// stub txAttr to get txManager
		TransactionAttribute txAttr = attr();

		// create first context
		AnnotationConfigApplicationContext ctx1 = new AnnotationConfigApplicationContext();

		// this config will create default transaction manager
		ctx1.register(Config.class, AspectJTxConfig.class, ConfigWithTxManagementConfigurer.class);
		ctx1.refresh();

		// get aspect bean (it's a real singleton, it's one per jvm)
		TransactionAspectSupport supportBean = ctx1.getBeansOfType(TransactionAspectSupport.class).get(TRANSACTION_MANAGER_KEY);
		TransactionAspectSupport supportBeanJavax = ctx1.getBeansOfType(TransactionAspectSupport.class).get(JAVAX_TRANSACTION_MANAGER_KEY);

		// get txManager
		final PlatformTransactionManager firstManager = supportBean.determineTransactionManager(txAttr);
		final PlatformTransactionManager firstManagerJavax = supportBeanJavax.determineTransactionManager(txAttr);

		// create second context, little different from first
		AnnotationConfigApplicationContext ctx2 = new AnnotationConfigApplicationContext();

		// this config is without default transaction manager, so it shouldn't be in aspect
		ctx2.register(Config.class, AspectJTxConfig.class, SomeOtherConfig.class);
		ctx2.refresh();

		// get new txManager
		final PlatformTransactionManager secondManager = supportBean.determineTransactionManager(txAttr);
		final PlatformTransactionManager secondManagerJavax = supportBeanJavax.determineTransactionManager(txAttr);

		// check that txManager is not the same
		assertThat(firstManager).isNotEqualTo(secondManager);
		assertThat(firstManagerJavax).isNotEqualTo(secondManagerJavax);
	}

	@Configuration
	@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
	static class Config {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.HSQL)
					.build();
		}

		@Bean
		JdbcFooRepository repository() {
			return new JdbcFooRepository();
		}
	}

	@Configuration
	static class AspectJTxConfig {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}

	@Configuration
	static class SomeOtherConfig {

	}

	static class ConfigWithTxManagementConfigurer {
		@Bean
		TransactionManagementConfigurer configurer(DataSource dataSource) {
			return new TransactionManagementConfigurer() {
				@Override
				public TransactionManager annotationDrivenTransactionManager() {
					return new DataSourceTransactionManager(dataSource);
				}
			};
		}
	}

	@Repository
	static class JdbcFooRepository {

		@org.springframework.transaction.annotation.Transactional
		public List<Object> findAll() {
			return Collections.emptyList();
		}

		@javax.transaction.Transactional
		public List<Object> findAllJta() {
			return Collections.emptyList();
		}
	}

	private TransactionAttribute attr() {
		return new TransactionAttribute() {

			@Override
			public String getQualifier() {
				return "";
			}

			@Override
			public boolean rollbackOn(Throwable ex) {
				return false;
			}
		};
	}

}
