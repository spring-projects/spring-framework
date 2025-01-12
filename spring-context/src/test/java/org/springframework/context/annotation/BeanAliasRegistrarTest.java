package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

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

		Object beanCreateByOtherProject = context.getBean("beanCreateByOtherProject");
		assertThat(beanCreateByOtherProject).isNotNull();
		Object driver = context.getBean("driver");
		assertThat(driver).isNotNull();
		assertThat(driver).isSameAs(beanCreateByOtherProject);
		Object bean1 = context.getBean("bean1");
		assertThat(bean1).isNotNull();
		assertThat(bean1).isSameAs(beanCreateByOtherProject);
		Object bean2 = context.getBean("bean2");
		assertThat(bean2).isNotNull();
		assertThat(bean2).isSameAs(beanCreateByOtherProject);
		Object beanCreateInThisProject = context.getBean("beanCreateInThisProject");
		assertThat(beanCreateInThisProject).isNotNull();
		assertThat(beanCreateInThisProject).isEqualTo("working with otherProjectDriver");
	}

	@BeanAlias(name = "beanCreateByOtherProject", alias = "driver")
	@BeanAliases({
			@BeanAlias(name = "beanCreateByOtherProject", alias = "bean1"),
			@BeanAlias(name = "beanCreateByOtherProject", alias = "bean2")
	})
	static class ConfigOfThisProject {

		@Bean
		String beanCreateInThisProject(@Qualifier("driver") String driver) {
			return "working with " + driver;
		}

	}

	static class ConfigFromOtherProject {

		@Bean
		String beanCreateByOtherProject() {
			return "otherProjectDriver";
		}

	}


}
