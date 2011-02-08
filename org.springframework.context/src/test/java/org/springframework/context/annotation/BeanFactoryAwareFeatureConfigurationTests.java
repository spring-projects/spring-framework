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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

/**
 * Tests that @FeatureConfiguration classes may implement Aware interfaces,
 * such as BeanFactoryAware.  This is not generally recommended but occasionally
 * useful, particularly in testing.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class BeanFactoryAwareFeatureConfigurationTests {
	@Test
	public void test() {
		ConfigurableApplicationContext ctx =
			new AnnotationConfigApplicationContext(FeatureConfig.class);
		FeatureConfig fc = ctx.getBean(FeatureConfig.class);
		assertThat(fc.featureMethodWasCalled, is(true));
		assertThat(fc.gotBeanFactoryInTime, is(true));
		assertThat(fc.beanFactory, is(ctx.getBeanFactory()));
	}

	@FeatureConfiguration
	static class FeatureConfig implements BeanFactoryAware {

		ConfigurableListableBeanFactory beanFactory;
		boolean featureMethodWasCalled = false;
		boolean gotBeanFactoryInTime = false;

		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory)beanFactory;
		}

		@Feature
		public FeatureSpecification f() {
			this.featureMethodWasCalled = true;
			this.gotBeanFactoryInTime = (this.beanFactory != null);
			return new StubSpecification();
		}
	}
}
