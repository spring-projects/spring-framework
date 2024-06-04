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

package org.springframework.orm.jpa.persistenceunit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.hibernate.tuple.CreationTimestampGeneration;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.domain.DriversLicense;
import org.springframework.orm.jpa.domain.Employee;
import org.springframework.orm.jpa.domain.EmployeeCategoryConverter;
import org.springframework.orm.jpa.domain.EmployeeId;
import org.springframework.orm.jpa.domain.EmployeeKindConverter;
import org.springframework.orm.jpa.domain.EmployeeLocation;
import org.springframework.orm.jpa.domain.EmployeeLocationConverter;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.orm.jpa.domain.PersonListener;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PersistenceManagedTypesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class PersistenceManagedTypesBeanRegistrationAotProcessorTests {

	@Test
	void processEntityManagerWithPackagesToScan() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(JpaDomainConfiguration.class);
		compile(context, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			PersistenceManagedTypes persistenceManagedTypes = freshApplicationContext.getBean(
					"persistenceManagedTypes", PersistenceManagedTypes.class);
			assertThat(persistenceManagedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
					DriversLicense.class.getName(), Person.class.getName(), Employee.class.getName(),
					EmployeeLocationConverter.class.getName());
			assertThat(persistenceManagedTypes.getManagedPackages()).isEmpty();
			assertThat(freshApplicationContext.getBean(
					JpaDomainConfiguration.class).scanningInvoked).isFalse();
		});
	}

	@Test
	void contributeJpaHints() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(JpaDomainConfiguration.class);
		contributeHints(context, hints -> {
			assertThat(RuntimeHintsPredicates.reflection().onType(DriversLicense.class)
					.withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(Person.class)
					.withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(PersonListener.class)
					.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS))
					.accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(Employee.class)
					.withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onMethod(Employee.class, "preRemove"))
					.accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeId.class)
					.withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeLocationConverter.class)
					.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeCategoryConverter.class)
					.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeKindConverter.class)
					.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
			assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeLocation.class)
					.withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
		});
	}

	@Test
	void contributeHibernateHints() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(HibernateDomainConfiguration.class);
		contributeHints(context, hints ->
				assertThat(RuntimeHintsPredicates.reflection().onType(CreationTimestampGeneration.class)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints));
	}


	@SuppressWarnings("unchecked")
	private void compile(GenericApplicationContext applicationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}

	private void contributeHints(GenericApplicationContext applicationContext, Consumer<RuntimeHints> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		result.accept(generationContext.getRuntimeHints());
	}

	public static class JpaDomainConfiguration extends AbstractEntityManagerWithPackagesToScanConfiguration {

		@Override
		protected String packageToScan() {
			return "org.springframework.orm.jpa.domain";
		}
	}

	public static class HibernateDomainConfiguration extends AbstractEntityManagerWithPackagesToScanConfiguration {

		@Override
		protected String packageToScan() {
			return "org.springframework.orm.jpa.hibernate.domain";
		}
	}

	public abstract static class AbstractEntityManagerWithPackagesToScanConfiguration {

		protected boolean scanningInvoked;

		@Bean
		public DataSource mockDataSource() {
			return mock();
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
					.scan(packageToScan());
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

		protected abstract String packageToScan();

	}

}
