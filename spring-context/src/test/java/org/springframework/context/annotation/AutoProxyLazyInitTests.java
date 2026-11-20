/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BeanNameAutoProxyCreator} and
 * {@link LazyInitTargetSourceCreator}.
 *
 * @author Juergen Hoeller
 * @author Arrault Fabien
 * @author Sam Brannen
 */
class AutoProxyLazyInitTests {

	@BeforeEach
	void resetBeans() {
		MyBeanImpl.initialized = false;
	}

	@Test
	void withStaticBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStatic.class);
		MyBean bean = ctx.getBean(MyBean.class);

		assertThat(MyBeanImpl.initialized).isFalse();
		bean.doIt();
		assertThat(MyBeanImpl.initialized).isTrue();

		ctx.close();
	}

	@Test
	void withStaticBeanMethodAndInterface() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStaticAndInterface.class);
		MyBean bean = ctx.getBean(MyBean.class);

		assertThat(MyBeanImpl.initialized).isFalse();
		bean.doIt();
		assertThat(MyBeanImpl.initialized).isTrue();

		ctx.close();
	}

	@Test
	void withNonStaticBeanMethod() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStatic.class);
		MyBean bean = ctx.getBean(MyBean.class);

		assertThat(MyBeanImpl.initialized).isFalse();
		bean.doIt();
		assertThat(MyBeanImpl.initialized).isTrue();

		ctx.close();
	}

	@Test
	void withNonStaticBeanMethodAndInterface() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStaticAndInterface.class);
		MyBean bean = ctx.getBean(MyBean.class);

		assertThat(MyBeanImpl.initialized).isFalse();
		bean.doIt();
		assertThat(MyBeanImpl.initialized).isTrue();

		ctx.close();
	}


	interface MyBean {

		String doIt();
	}


	static class MyBeanImpl implements MyBean {

		static boolean initialized = false;

		MyBeanImpl() {
			initialized = true;
		}

		@Override
		public String doIt() {
			return "From implementation";
		}

		@PreDestroy
		public void destroy() {
		}
	}


	@Configuration
	static class ConfigWithStatic {

		@Bean
		BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setBeanNames("*");
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		static MyBean myBean() {
			return new MyBeanImpl();
		}
	}


	@Configuration
	static class ConfigWithStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

		@Bean
		BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setBeanNames("*");
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		static MyBean myBean() {
			return new MyBeanImpl();
		}

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
		}
	}


	@Configuration
	static class ConfigWithNonStatic {

		@Bean
		BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setBeanNames("*");
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		MyBean myBean() {
			return new MyBeanImpl();
		}
	}


	@Configuration
	static class ConfigWithNonStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

		@Bean
		BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setBeanNames("*");
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		MyBean myBean() {
			return new MyBeanImpl();
		}

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
		}
	}


	private static class StrictLazyInitTargetSourceCreator extends LazyInitTargetSourceCreator {

		@Override
		protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(Class<?> beanClass, String beanName) {
			if ("myBean".equals(beanName)) {
				assertThat(beanClass).isEqualTo(MyBean.class);
			}
			return super.createBeanFactoryBasedTargetSource(beanClass, beanName);
		}
	}

}
