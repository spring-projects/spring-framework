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

package org.springframework.beans.factory;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Benchmark for creating prototype beans in a concurrent fashion.
 * This benchmark requires to customize the number of worker threads {@code -t <int>} on the
 * CLI when running this particular benchmark to leverage concurrency.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ConcurrentBeanFactoryBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		public DefaultListableBeanFactory factory;

		@Setup
		public void setup() {
			this.factory = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(
					qualifiedResource(ConcurrentBeanFactoryBenchmark.class, "context.xml"));

			this.factory.addPropertyEditorRegistrar(
					registry -> registry.registerCustomEditor(Date.class,
							new CustomDateEditor(new SimpleDateFormat("yyyy/MM/dd"), false)));
		}

	}

	@Benchmark
	public void concurrentBeanCreation(BenchmarkState state, Blackhole bh) {
		bh.consume(state.factory.getBean("bean1"));
		bh.consume(state.factory.getBean("bean2"));
	}


	public static class ConcurrentBean {

		private Date date;

		public Date getDate() {
			return this.date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}
}
