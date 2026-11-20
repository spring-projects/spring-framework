/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

/**
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
class ConverterFactoryNullnessTests {

	@Test
	fun converterFactoryWithNullableTypes() {
		val factory = StringToNullableIdConverterFactory

		val userIdConverter: Converter<String, UserId?> = factory.getConverter(UserId::class.java)
		assertThat(userIdConverter.convert("42")).isEqualTo(UserId("42"))
	}

	@Test
	fun converterFactoryWithNonNullableTypes() {
		val factory = StringToIdConverterFactory

		val userIdConverter: Converter<String, UserId> = factory.getConverter(UserId::class.java)
		assertThat(userIdConverter.convert("42")).isEqualTo(UserId("42"))
	}

	object StringToNullableIdConverterFactory : ConverterFactory<String, Id> {
		override fun <T : Id> getConverter(targetType: Class<T>): Converter<String, T?> {
			val constructor = checkNotNull(targetType.kotlin.primaryConstructor)
			return Converter { source ->
				constructor.call(source)
			}
		}
	}

	object StringToIdConverterFactory : ConverterFactory<String, Id> {
		override fun <T : Id> getConverter(targetType: Class<T>): Converter<String, T> {
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

}