/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BeanUtilsBenchmark {

	private Constructor<TestClass1> noArgConstructor;
	private Constructor<TestClass2> constructor;

	@Setup
	public void setUp() throws NoSuchMethodException {
		this.noArgConstructor = TestClass1.class.getDeclaredConstructor();
		this.constructor = TestClass2.class.getDeclaredConstructor(int.class, String.class);
	}

	@Benchmark
	public Object emptyConstructor() {
		return BeanUtils.instantiateClass(this.noArgConstructor);
	}

	@Benchmark
	public Object nonEmptyConstructor() {
		return BeanUtils.instantiateClass(this.constructor, 1, "str");
	}

	static class TestClass1 {
	}

	@SuppressWarnings("unused")
	static class TestClass2 {
		private final int value1;
		private final String value2;

		TestClass2(int value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}
	}

}
