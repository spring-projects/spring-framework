/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
