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
