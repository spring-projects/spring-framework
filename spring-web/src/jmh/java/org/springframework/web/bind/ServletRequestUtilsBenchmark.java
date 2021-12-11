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

package org.springframework.web.bind;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;


/**
 * Benchmarks for extracting parameters from {@libnk ServletRequest}.
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
