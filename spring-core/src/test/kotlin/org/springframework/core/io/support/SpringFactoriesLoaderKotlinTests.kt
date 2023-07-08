/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.io.support

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.io.support.SpringFactoriesLoader.FactoryInstantiator
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver

/**
 * Kotlin tests for [SpringFactoriesLoader].
 *
 * @author Phillip Webb
 */
@Suppress("unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class SpringFactoriesLoaderKotlinTests {

	@Test
	fun `Instantiate immutable data class`() {
		val resolver = ArgumentResolver.of(java.lang.String::class.java, "test" as java.lang.String)
				.and(Integer.TYPE, 123)
		val instantiator = FactoryInstantiator.forClass<Immutable>(Immutable::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isEqualTo(Immutable("test", 123))
	}

	@Test
	fun `Instantiate immutable data class with optional parameter and all arguments specified`() {
		val resolver = ArgumentResolver.of(java.lang.String::class.java, "test" as java.lang.String)
		val instantiator = FactoryInstantiator.forClass<OneOptionalParameter>(OneOptionalParameter::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isEqualTo(OneOptionalParameter("test", 12))
	}

	@Test
	fun `Instantiate immutable class with optional argument and only mandatory arguments specified`() {
		val resolver = ArgumentResolver.of(java.lang.String::class.java, "test" as java.lang.String)
			.and(Integer.TYPE, 345)
		val instantiator = FactoryInstantiator.forClass<OneOptionalParameter>(OneOptionalParameter::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isEqualTo(OneOptionalParameter("test", 345))
	}

	@Test
	fun `Instantiate immutable class with nullable argument`() {
		val resolver = ArgumentResolver.of(java.lang.String::class.java, "test" as java.lang.String)
		val instantiator = FactoryInstantiator.forClass<NullableParameter>(NullableParameter::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isEqualTo(NullableParameter("test", null))
	}

	@Test
	fun `Instantiate class with all optional argument`() {
		val resolver = ArgumentResolver.none()
		val instantiator = FactoryInstantiator.forClass<AllOptionalParameters>(AllOptionalParameters::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isEqualTo(AllOptionalParameters())
	}

	@Test
	@Suppress("UsePropertyAccessSyntax")
	fun `Instantiate class with private constructor`() {
		val resolver = ArgumentResolver.none()
		val instantiator = FactoryInstantiator.forClass<PrivateConstructor>(PrivateConstructor::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isNotNull()
	}

	@Test
	fun `Instantiate class with protected constructor`() {
		val resolver = ArgumentResolver.none()
		val instantiator = FactoryInstantiator.forClass<ProtectedConstructor>(ProtectedConstructor::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isNotNull()
	}

	@Test
	fun `Instantiate private class`() {
		val resolver = ArgumentResolver.none()
		val instantiator = FactoryInstantiator.forClass<PrivateClass>(PrivateClass::class.java)
		val instance = instantiator.instantiate(resolver)
		assertThat(instance).isNotNull()
	}

	data class Immutable(val param1: String, val param2: Int)

	data class OneOptionalParameter(val param1: String, val param2: Int = 12)

	data class AllOptionalParameters(var param1: String = "a", var param2: Int = 12)

	data class NullableParameter(val param1: String, val param2: Int?)

	class PrivateConstructor private constructor()

	open class ProtectedConstructor protected constructor()

	private class PrivateClass

}
