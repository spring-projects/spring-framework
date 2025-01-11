package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests for {@link BeanAliasRegistrar}.
 *
 * <p>This class contains unit tests to verify the functionality of the {@link BeanAliasRegistrar}
 * class, ensuring that bean aliases are correctly registered based on the {@link BeanAlias} and
 * {@link BeanAliases} annotations.
 *
 * @author Tiger Zhao
 * @since 7.0.0
 */
class BeanAliasRegistrarTest {

	@Test
	public void test() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ConfigOfThisProject.class);
		context.register(ConfigFromOtherProject.class);
		context.refresh();

		Object bean0 = context.getBean("bean0");
		assertThat(bean0).isNotNull();
		Object bean1 = context.getBean("bean1");
		assertThat(bean1).isNotNull();
		assertThat(bean1).isSameAs(bean0);
		Object bean2 = context.getBean("bean2");
		assertThat(bean2).isNotNull();
		assertThat(bean2).isSameAs(bean0);
		Object bean3 = context.getBean("bean3");
		assertThat(bean3).isNotNull();
		assertThat(bean3).isSameAs(bean0);
	}

	@BeanAlias(name = "bean0", alias = "bean1")
	@BeanAliases({
			@BeanAlias(name = "bean0", alias = "bean2"),
			@BeanAlias(name = "bean0", alias = "bean3")
	})
	static class ConfigOfThisProject {

	}

	static class ConfigFromOtherProject {

		@Bean
		String bean0() {
			return "";
		}

	}


}
