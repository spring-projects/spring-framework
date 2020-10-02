/*
 * Copyright 2002-2020 the original author or authors.
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

import javax.annotation.Resource;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Benchmark for bean annotation processing with various annotations.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class AnnotationProcessorBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		public GenericApplicationContext context;

		@Param({"ResourceAnnotatedTestBean", "AutowiredAnnotatedTestBean"})
		public String testBeanClass;

		@Param({"true", "false"})
		public boolean overridden;

		@Setup
		public void setup() {
			RootBeanDefinition rbd;
			this.context = new GenericApplicationContext();
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.context);
			this.context.refresh();
			if (this.testBeanClass.equals("ResourceAnnotatedTestBean")) {
				rbd = new RootBeanDefinition(ResourceAnnotatedTestBean.class);
			}
			else {
				rbd = new RootBeanDefinition(AutowiredAnnotatedTestBean.class);
			}
			rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			if (this.overridden) {
				rbd.getPropertyValues().add("spouse", new RuntimeBeanReference("spouse"));
			}
			this.context.registerBeanDefinition("test", rbd);
			this.context.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));
		}
	}

	@Benchmark
	public ITestBean prototypeCreation(BenchmarkState state) {
		TestBean tb = state.context.getBean("test", TestBean.class);
		return tb.getSpouse();
	}


	private static class ResourceAnnotatedTestBean extends org.springframework.beans.testfixture.beans.TestBean {

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
