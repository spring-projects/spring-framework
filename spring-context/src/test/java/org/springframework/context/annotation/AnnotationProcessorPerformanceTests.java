/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.Assume;
import org.springframework.tests.EnabledForTestGroups;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.tests.TestGroup.PERFORMANCE;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 2.5
 */
@EnabledForTestGroups(PERFORMANCE)
public class AnnotationProcessorPerformanceTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);


	@BeforeAll
	public static void commonAssumptions() {
		Assume.notLogging(factoryLog);
	}

	@Test
	public void prototypeCreationWithResourcePropertiesIsFastEnough() {
		GenericApplicationContext ctx = createContext();

		RootBeanDefinition rbd = new RootBeanDefinition(ResourceAnnotatedTestBean.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ctx.registerBeanDefinition("test", rbd);
		ctx.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));

		assertFastEnough(ctx);
	}

	@Test
	public void prototypeCreationWithOverriddenResourcePropertiesIsFastEnough() {
		GenericApplicationContext ctx = createContext();

		RootBeanDefinition rbd = new RootBeanDefinition(ResourceAnnotatedTestBean.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("spouse", new RuntimeBeanReference("spouse"));
		ctx.registerBeanDefinition("test", rbd);
		ctx.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));

		assertFastEnough(ctx);
	}

	@Test
	public void prototypeCreationWithAutowiredPropertiesIsFastEnough() {
		GenericApplicationContext ctx = createContext();

		RootBeanDefinition rbd = new RootBeanDefinition(AutowiredAnnotatedTestBean.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ctx.registerBeanDefinition("test", rbd);
		ctx.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));

		assertFastEnough(ctx);
	}

	@Test
	public void prototypeCreationWithOverriddenAutowiredPropertiesIsFastEnough() {
		GenericApplicationContext ctx = createContext();

		RootBeanDefinition rbd = new RootBeanDefinition(AutowiredAnnotatedTestBean.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("spouse", new RuntimeBeanReference("spouse"));
		ctx.registerBeanDefinition("test", rbd);
		ctx.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));

		assertFastEnough(ctx);
	}

	private GenericApplicationContext createContext() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ctx);
		ctx.refresh();
		return ctx;
	}

	private void assertFastEnough(GenericApplicationContext ctx) {
		AtomicBoolean done = new AtomicBoolean();
		TestBean spouse = ctx.getBean("spouse", TestBean.class);
		Executors.newSingleThreadExecutor().submit(() -> {
			for (int i = 0; i < 100_000; i++) {
				TestBean tb = ctx.getBean("test", TestBean.class);
				assertThat(tb.getSpouse()).isSameAs(spouse);
			}
			done.set(true);
		});

		// "fast enough" is of course relative, but we're using 6 seconds with the hope
		// that these tests typically pass on the CI server.
		Awaitility.await()
			.atMost(6, TimeUnit.SECONDS)
			.pollInterval(100, TimeUnit.MILLISECONDS)
			.untilTrue(done);
	}


	private static class ResourceAnnotatedTestBean extends TestBean {

		@Override
		@Resource
		@SuppressWarnings("deprecation")
		@org.springframework.beans.factory.annotation.Required
		public void setSpouse(ITestBean spouse) {
			super.setSpouse(spouse);
		}
	}

	private static class AutowiredAnnotatedTestBean extends TestBean {

		@Override
		@Autowired
		@SuppressWarnings("deprecation")
		@org.springframework.beans.factory.annotation.Required
		public void setSpouse(ITestBean spouse) {
			super.setSpouse(spouse);
		}
	}

}
