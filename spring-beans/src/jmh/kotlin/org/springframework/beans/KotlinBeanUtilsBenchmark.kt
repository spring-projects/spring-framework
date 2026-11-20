/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans

import java.util.concurrent.TimeUnit

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

	class TestClass1

	@Suppress("UNUSED_PARAMETER")
	class TestClass2(int: Int, string: String)
}

