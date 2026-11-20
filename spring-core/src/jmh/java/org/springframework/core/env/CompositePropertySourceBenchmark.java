/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

/**
 * Benchmarks for {@link CompositePropertySource}.
 *
 * @author Yike Xiao
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
public class CompositePropertySourceBenchmark {

	@Benchmark
	public void getPropertyNames(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.composite.getPropertyNames());
	}

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		private static final IdGenerator ID_GENERATOR = new AlternativeJdkIdGenerator();

		private static final Object VALUE = new Object();

		CompositePropertySource composite;

		@Param({ "2", "5", "10" })
		int numberOfPropertySources;

		@Param({ "10", "100", "1000" })
		int numberOfPropertyNamesPerSource;

		@Setup(Level.Trial)
		public void setUp() {
			this.composite = new CompositePropertySource("benchmark");
			for (int i = 0; i < this.numberOfPropertySources; i++) {
				Map<String, Object> map = new HashMap<>(this.numberOfPropertyNamesPerSource);
				for (int j = 0; j < this.numberOfPropertyNamesPerSource; j++) {
					map.put(ID_GENERATOR.generateId().toString(), VALUE);
				}
				PropertySource<?> propertySource = new MapPropertySource("propertySource" + i, map);
				this.composite.addPropertySource(propertySource);
			}
		}

	}

}
