/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import jakarta.servlet.ServletRequest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;


/**
 * Benchmarks for extracting parameters from {@link ServletRequest}.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ServletRequestUtilsBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkData {

		public MockHttpServletRequest request = new MockHttpServletRequest();

		public String parameterName = "nonExistingParam";
	}

	@Benchmark
	public int intParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getIntParameter(data.request, data.parameterName, 0);
	}

	@Benchmark
	public long longParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getLongParameter(data.request, data.parameterName, 0);
	}

	@Benchmark
	public float floatParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getFloatParameter(data.request, data.parameterName, 0f);
	}

	@Benchmark
	public double doubleParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getDoubleParameter(data.request, data.parameterName, 0d);
	}

	@Benchmark
	public boolean booleanParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getBooleanParameter(data.request, data.parameterName, false);
	}


	@Benchmark
	public String stringParameterWithDefaultValue(BenchmarkData data) {
		return ServletRequestUtils.getStringParameter(data.request, data.parameterName, "defaultValue");
	}

}
