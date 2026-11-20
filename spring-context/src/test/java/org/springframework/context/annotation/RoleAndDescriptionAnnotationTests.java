/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
