package org.springframework.beans.factory.aspectj;

import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import static org.junit.Assert.assertNotNull;

public class AnnotationBeanConfigurerAspectTest {
	@Test
	public void injection() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			final MyLoadTimeWeaverAware myLoadTimeWeaverAware = context.getBean(MyLoadTimeWeaverAware.class);
			final MyConfiguredBean myConfiguredBean = myLoadTimeWeaverAware.getMyConfiguredBean();
			assertNotNull(myConfiguredBean);
			final Foo foo = myConfiguredBean.getFoo();
			assertNotNull(foo);
		}
	}

	@Configuration
	@EnableSpringConfigured
	static class Config {
		@Bean
		@DependsOn("org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect")
		public MyLoadTimeWeaverAware myLoadTimeWeaverAware() {
			return new MyLoadTimeWeaverAware();
		}

		@Bean
		public Foo foo() {
			return new Foo();
		}
	}

	@Configurable
	static class MyConfiguredBean {
		@Autowired
		private Foo foo;

		public Foo getFoo() {
			return foo;
		}
	}

	static class Foo {
	}

	static class MyLoadTimeWeaverAware implements LoadTimeWeaverAware, InitializingBean {
		private MyConfiguredBean myConfiguredBean;

		public MyConfiguredBean getMyConfiguredBean() {
			return myConfiguredBean;
		}

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
			// no action, just to be visible for Spring's early initialization of LoadTimeWeaverAware
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.myConfiguredBean = new MyConfiguredBean();
		}
	}
}
