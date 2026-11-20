/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

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
			System.clearProperty("country");
			System.clearProperty("name");
		}
	}

	@Benchmark
	public void prototypeCreationWithSystemProperties(BenchmarkState state, Blackhole bh) {
		TestBean tb = (TestBean) state.context.getBean("test");
		bh.consume(tb.getName());
		bh.consume(tb.getCountry());
	}
}
