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

package org.springframework.context.expression;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Benchmark for application context expressions resolution during prototype bean creation.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ApplicationContextExpressionBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		public GenericApplicationContext context;

		@Setup
		public void setup() {
			System.getProperties().put("name", "juergen");
			System.getProperties().put("country", "UK");
			this.context = new GenericApplicationContext();
			RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
			rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			rbd.getConstructorArgumentValues().addGenericArgumentValue("#{systemProperties.name}");
			rbd.getPropertyValues().add("country", "#{systemProperties.country}");
			this.context.registerBeanDefinition("test", rbd);
			this.context.refresh();
		}

		@TearDown
		public void teardown() {
			System.getProperties().remove("country");
			System.getProperties().remove("name");
		}
	}

	@Benchmark
	public void prototypeCreationWithSystemProperties(BenchmarkState state, Blackhole bh) {
		TestBean tb = (TestBean) state.context.getBean("test");
		bh.consume(tb.getName());
		bh.consume(tb.getCountry());
	}
}
