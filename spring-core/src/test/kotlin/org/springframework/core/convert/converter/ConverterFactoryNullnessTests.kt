/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.core.convert.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

/**
 * @author Brian Clozel
 */
class ConverterFactoryNullnessTests {

	@Test
	fun converterFactoryWithNullableTypes() {
		val factory = StringToIdConverterFactory

		val userIdConverter = factory.getConverter(UserId::class.java)
		assertThat(userIdConverter.convert("42")).isEqualTo(UserId("42"))
	}

	object StringToIdConverterFactory : ConverterFactory<String, Id> {
		override fun <T : Id> getConverter(targetType: Class<T>): Converter<String, T?> {
			val constructor = checkNotNull(targetType.kotlin.primaryConstructor)
			return Converter { source ->
				constructor.call(source)
			}
		}
	}

	abstract class Id {
		abstract val value: String
	}

	data class UserId(override val value: String) : Id()

	data class ProductId(override val value: String) : Id()

}