/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import java.util.function.BiConsumer;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.aot.test.generate.compile.Compiled;
import org.springframework.aot.test.generate.compile.TestCompiler;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PersistenceManagedTypesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class PersistenceManagedTypesBeanRegistrationAotProcessorTests {

	@Test
	void processEntityManagerWithPackagesToScan() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(EntityManagerWithPackagesToScanConfiguration.class);
		compile(context, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			PersistenceManagedTypes persistenceManagedTypes = freshApplicationContext.getBean(
					"persistenceManagedTypes", PersistenceManagedTypes.class);
			assertThat(persistenceManagedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
					DriversLicense.class.getName(), Person.class.getName());
			assertThat(persistenceManagedTypes.getManagedPackages()).isEmpty();
			assertThat(freshApplicationContext.getBean(
					EntityManagerWithPackagesToScanConfiguration.class).scanningInvoked).isFalse();
		});
	}


	@SuppressWarnings("unchecked")
	private void compile(GenericApplicationContext applicationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}

	@Configuration(proxyBeanMethods = false)
	public static class EntityManagerWithPackagesToScanConfiguration {

		private boolean scanningInvoked;

		@Bean
		public DataSource mockDataSource() {
			return mock(DataSource.class);
		}

		@Bean
		public HibernateJpaVendorAdapter jpaVendorAdapter() {
			HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
			jpaVendorAdapter.setDatabase(Database.HSQL);
			return jpaVendorAdapter;
		}

		@Bean
		public PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) {
			this.scanningInvoked = true;
			return new PersistenceManagedTypesScanner(resourceLoader)
					.scan("org.springframework.orm.jpa.domain");
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
				JpaVendorAdapter jpaVendorAdapter, PersistenceManagedTypes persistenceManagedTypes) {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
			entityManagerFactoryBean.setDataSource(dataSource);
			entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
			entityManagerFactoryBean.setManagedTypes(persistenceManagedTypes);
			return entityManagerFactoryBean;
		}

	}

}
