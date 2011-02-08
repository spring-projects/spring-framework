/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Tests that @Bean methods referenced from within @Feature methods
 * get proxied early to avoid premature instantiation of actual
 * bean instances.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureMethodEarlyBeanProxyTests {

	@Test
	public void earlyProxyCreationAndBeanRegistrationLifecycle() {
		AnnotationConfigApplicationContext ctx =
			new AnnotationConfigApplicationContext(FeatureConfig.class);

		//
		// see additional assertions in FeatureConfig#feature()
		//

		// sanity check that all the bean definitions we expecting are present
		assertThat(ctx.getBeanFactory().containsBeanDefinition("lazyHelperBean"), is(true));
		assertThat(ctx.getBeanFactory().containsBeanDefinition("eagerHelperBean"), is(true));
		assertThat(ctx.getBeanFactory().containsBeanDefinition("lazyPassthroughBean"), is(true));
		assertThat(ctx.getBeanFactory().containsBeanDefinition("eagerPassthroughBean"), is(true));


		// the lazy helper bean had methods invoked during feature method execution. it should be registered
		assertThat(ctx.getBeanFactory().containsSingleton("lazyHelperBean"), is(true));

		// the eager helper bean had methods invoked but should be registered in any case is it is non-lazy
		assertThat(ctx.getBeanFactory().containsSingleton("eagerHelperBean"), is(true));

		// the lazy passthrough bean was referenced in the feature method, but never invoked. it should not be registered
		assertThat(ctx.getBeanFactory().containsSingleton("lazyPassthroughBean"), is(false));

		// the eager passthrough bean should be registered in any case as it is non-lazy
		assertThat(ctx.getBeanFactory().containsSingleton("eagerPassthroughBean"), is(true));


		// now actually fetch all the beans. none should be proxies
		assertThat(ctx.getBean("lazyHelperBean"), not(instanceOf(EarlyBeanReferenceProxy.class)));
		assertThat(ctx.getBean("eagerHelperBean"), not(instanceOf(EarlyBeanReferenceProxy.class)));
		assertThat(ctx.getBean("lazyPassthroughBean"), not(instanceOf(EarlyBeanReferenceProxy.class)));
		assertThat(ctx.getBean("eagerPassthroughBean"), not(instanceOf(EarlyBeanReferenceProxy.class)));
	}


	@Test
	public void earlyProxyBeansMayBeInterfaceBasedOrConcrete() {
		new AnnotationConfigApplicationContext(FeatureConfigReferencingNonInterfaceBeans.class);
	}
}


@FeatureConfiguration
@Import(TestBeanConfig.class)
class FeatureConfig implements BeanFactoryAware {
	private DefaultListableBeanFactory beanFactory;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (DefaultListableBeanFactory)beanFactory;
	}

	@Feature
	public StubSpecification feature(TestBeanConfig beans) {

		assertThat(
				"The @Configuration class instance itself should be an early-ref proxy",
				beans, instanceOf(EarlyBeanReferenceProxy.class));

		// invocation of @Bean methods within @Feature methods should return proxies
		ITestBean lazyHelperBean = beans.lazyHelperBean();
		ITestBean eagerHelperBean = beans.eagerHelperBean();
		ITestBean lazyPassthroughBean = beans.lazyPassthroughBean();
		ITestBean eagerPassthroughBean = beans.eagerPassthroughBean();

		assertThat(lazyHelperBean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(eagerHelperBean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(lazyPassthroughBean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(eagerPassthroughBean, instanceOf(EarlyBeanReferenceProxy.class));

		// but at this point, the proxy instances should not have
		// been registered as singletons with the container.
		assertThat(this.beanFactory.containsSingleton("lazyHelperBean"), is(false));
		assertThat(this.beanFactory.containsSingleton("eagerHelperBean"), is(false));
		assertThat(this.beanFactory.containsSingleton("lazyPassthroughBean"), is(false));
		assertThat(this.beanFactory.containsSingleton("eagerPassthroughBean"), is(false));

		// invoking a method on the proxy should cause it to pass through
		// to the container, instantiate the actual bean in question and
		// register that actual underlying instance as a singleton.
		assertThat(lazyHelperBean.getName(), equalTo("lazyHelper"));
		assertThat(eagerHelperBean.getName(), equalTo("eagerHelper"));

		assertThat(this.beanFactory.containsSingleton("lazyHelperBean"), is(true));
		assertThat(this.beanFactory.containsSingleton("eagerHelperBean"), is(true));

		// since no methods were called on the passthrough beans, they should remain
		// uncreated / unregistered.
		assertThat(this.beanFactory.containsSingleton("lazyPassthroughBean"), is(false));
		assertThat(this.beanFactory.containsSingleton("eagerPassthroughBean"), is(false));

		return new StubSpecification();
	}
}


@Configuration
class TestBeanConfig {

	@Lazy @Bean
	public ITestBean lazyHelperBean() {
		return new TestBean("lazyHelper");
	}

	@Bean
	public ITestBean eagerHelperBean() {
		return new TestBean("eagerHelper");
	}

	@Lazy @Bean
	public ITestBean lazyPassthroughBean() {
		return new TestBean("lazyPassthrough");
	}

	@Bean
	public ITestBean eagerPassthroughBean() {
		return new TestBean("eagerPassthrough");
	}

}


@FeatureConfiguration
@Import(NonInterfaceBeans.class)
class FeatureConfigReferencingNonInterfaceBeans {
	@Feature
	public FeatureSpecification feature1(NonInterfaceBeans beans) throws Throwable {
		beans.testBean();
		return new StubSpecification();
	}

	@Feature
	public FeatureSpecification feature2(TestBean testBean) throws Throwable {
		return new StubSpecification();
	}
}


@Configuration
class NonInterfaceBeans {
	@Bean
	public TestBean testBean() {
		return new TestBean("invalid");
	}
}

