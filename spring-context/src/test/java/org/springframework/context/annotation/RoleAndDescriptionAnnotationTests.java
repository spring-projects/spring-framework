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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.role.ComponentWithRole;
import org.springframework.context.annotation.role.ComponentWithoutRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the use of the @Role and @Description annotation on @Bean methods and @Component classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class RoleAndDescriptionAnnotationTests {

	@Test
	void onBeanMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class);
		ctx.refresh();
		assertThat(ctx.getBeanDefinition("foo").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
		assertThat(ctx.getBeanDefinition("foo").getDescription()).isNull();
		assertThat(ctx.getBeanDefinition("bar").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertThat(ctx.getBeanDefinition("bar").getDescription()).isEqualTo("A Bean method with a role");
		ctx.close();
	}

	@Test
	void onComponentClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentWithoutRole.class, ComponentWithRole.class);
		ctx.refresh();
		assertThat(ctx.getBeanDefinition("componentWithoutRole").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
		assertThat(ctx.getBeanDefinition("componentWithoutRole").getDescription()).isNull();
		assertThat(ctx.getBeanDefinition("componentWithRole").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertThat(ctx.getBeanDefinition("componentWithRole").getDescription()).isEqualTo("A Component with a role");
		ctx.close();
	}

	@Test
	void viaComponentScanning() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("org.springframework.context.annotation.role");
		ctx.refresh();
		assertThat(ctx.getBeanDefinition("componentWithoutRole").getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
		assertThat(ctx.getBeanDefinition("componentWithoutRole").getDescription()).isNull();
		assertThat(ctx.getBeanDefinition("componentWithRole").getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertThat(ctx.getBeanDefinition("componentWithRole").getDescription()).isEqualTo("A Component with a role");
		ctx.close();
	}


	@Configuration
	static class Config {
		@Bean
		String foo() {
			return "foo";
		}

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		@Description("A Bean method with a role")
		String bar() {
			return "bar";
		}
	}

}
