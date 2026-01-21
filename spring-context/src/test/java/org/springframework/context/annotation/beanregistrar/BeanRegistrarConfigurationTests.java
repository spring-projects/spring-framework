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

package org.springframework.context.annotation.beanregistrar;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.testfixture.beans.factory.BarRegistrar;
import org.springframework.context.testfixture.beans.factory.FooRegistrar;
import org.springframework.context.testfixture.beans.factory.GenericBeanRegistrar;
import org.springframework.context.testfixture.beans.factory.ImportAwareBeanRegistrar;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Bar;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Baz;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Foo;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Init;
import org.springframework.context.testfixture.context.annotation.registrar.BeanRegistrarConfiguration;
import org.springframework.context.testfixture.context.annotation.registrar.GenericBeanRegistrarConfiguration;
import org.springframework.context.testfixture.context.annotation.registrar.ImportAwareBeanRegistrarConfiguration;
import org.springframework.context.testfixture.context.annotation.registrar.MultipleBeanRegistrarsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BeanRegistrar} imported by @{@link org.springframework.context.annotation.Configuration}.
 *
 * @author Sebastien Deleuze
 */
public class BeanRegistrarConfigurationTests {

	@Test
	void beanRegistrar() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanRegistrarConfiguration.class);
		assertThat(context.getBean(Bar.class).foo()).isEqualTo(context.getBean(Foo.class));
		assertThat(context.getBean("foo", Foo.class)).isEqualTo(context.getBean("fooAlias", Foo.class));
		assertThatThrownBy(() -> context.getBean(Baz.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThat(context.getBean(Init.class).initialized).isTrue();
		BeanDefinition beanDefinition = context.getBeanDefinition("bar");
		assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
		assertThat(beanDefinition.isLazyInit()).isTrue();
		assertThat(beanDefinition.getDescription()).isEqualTo("Custom description");
	}

	@Test
	void beanRegistrarWithProfile() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(BeanRegistrarConfiguration.class);
		context.getEnvironment().addActiveProfile("baz");
		context.refresh();
		assertThat(context.getBean(Baz.class).message()).isEqualTo("Hello World!");
	}

	@Test
	void scannedFunctionalConfiguration() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.scan("org.springframework.context.testfixture.context.annotation.registrar");
		context.refresh();
		assertThat(context.getBean(Bar.class).foo()).isEqualTo(context.getBean(Foo.class));
		assertThatThrownBy(() -> context.getBean(Baz.class).message()).isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThat(context.getBean(Init.class).initialized).isTrue();
	}

	@Test
	void beanRegistrarWithTargetType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(GenericBeanRegistrarConfiguration.class);
		context.refresh();
		RootBeanDefinition beanDefinition = (RootBeanDefinition)context.getBeanDefinition("fooSupplier");
		assertThat(beanDefinition.getResolvableType().resolveGeneric(0)).isEqualTo(GenericBeanRegistrar.Foo.class);
	}

	@Test
	void beanRegistrarWithImportAware() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ImportAwareBeanRegistrarConfiguration.class);
		context.refresh();
		assertThat(context.getBean(ImportAwareBeanRegistrar.ClassNameHolder.class).className())
				.isEqualTo(ImportAwareBeanRegistrarConfiguration.class.getName());
	}

	@Test
	void multipleBeanRegistrars() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MultipleBeanRegistrarsConfiguration.class);
		context.refresh();
		assertThat(context.getBean(FooRegistrar.Foo.class)).isNotNull();
		assertThat(context.getBean(BarRegistrar.Bar.class)).isNotNull();
	}

}
