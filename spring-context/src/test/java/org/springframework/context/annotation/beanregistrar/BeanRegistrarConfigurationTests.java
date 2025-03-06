/*
 * Copyright 2002-2025 the original author or authors.
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Bar;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Baz;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Foo;
import org.springframework.context.testfixture.beans.factory.SampleBeanRegistrar.Init;
import org.springframework.context.testfixture.context.annotation.registrar.BeanRegistrarConfiguration;

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

}
