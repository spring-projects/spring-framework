/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;

/**
 * Benchmark for {@link AbstractPropertyAccessor} use on beans.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class AbstractPropertyAccessorBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		@Param({"DirectFieldAccessor", "BeanWrapper"})
		public String accessor;

		@Param({"none", "stringTrimmer", "numberOnPath", "numberOnNestedPath", "numberOnType"})
		public String customEditor;

		public int[] input;

		public PrimitiveArrayBean target;

		public AbstractPropertyAccessor propertyAccessor;

		@Setup
		public void setup() {
			this.target = new PrimitiveArrayBean();
			this.input = new int[1024];
			if (this.accessor.equals("DirectFieldAccessor")) {
				this.propertyAccessor = new DirectFieldAccessor(this.target);
			}
			else {
				this.propertyAccessor = new BeanWrapperImpl(this.target);
			}
			switch (this.customEditor) {
				case "stringTrimmer" ->
					this.propertyAccessor.registerCustomEditor(String.class, new StringTrimmerEditor(false));
				case "numberOnPath" ->
					this.propertyAccessor.registerCustomEditor(int.class, "array.somePath", new CustomNumberEditor(Integer.class, false));
				case "numberOnNestedPath" ->
					this.propertyAccessor.registerCustomEditor(int.class, "array[0].somePath", new CustomNumberEditor(Integer.class, false));
				case "numberOnType" ->
					this.propertyAccessor.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, false));
			}
		}

	}

	@Benchmark
	public PrimitiveArrayBean setPropertyValue(BenchmarkState state) {
		state.propertyAccessor.setPropertyValue("array", state.input);
		return state.target;
	}

	@SuppressWarnings("unused")
	private static class PrimitiveArrayBean {

		private int[] array;

		public int[] getArray() {
			return this.array;
		}

		public void setArray(int[] array) {
			this.array = array;
		}
	}

}
