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

package org.springframework.beans

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class KotlinBeanUtilsBenchmark {

		private val noArgConstructor = TestClass1::class.java.getDeclaredConstructor()
		private val constructor = TestClass2::class.java.getDeclaredConstructor(Int::class.java, String::class.java)

		@Benchmark
		fun emptyConstructor(): Any {
		return BeanUtils.instantiateClass(noArgConstructor)
		}

		@Benchmark
		fun nonEmptyConstructor(): Any {
		return BeanUtils.instantiateClass(constructor, 1, "str")
		}

		class TestClass1()

		@Suppress("UNUSED_PARAMETER")
		class TestClass2(int: Int, string: String)
}

