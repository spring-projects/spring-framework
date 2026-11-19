/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.validation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.format.support.DefaultFormattingConversionService
import java.util.UUID

/**
 * Tests for [DataBinder] constructor binding with Kotlin value classes.
 */
class DataBinderKotlinValueClassTests {

	@Test
	fun constructDataClassWithStringValueClass() {
		val binder = createDataBinder(StringValueClassRecord::class.java)
		binder.construct(TestValueResolver(mapOf("title" to "hello")))

		Assertions.assertThat(getTarget<StringValueClassRecord>(binder).title).isEqualTo(Title("hello"))
	}

	@Test
	fun constructDataClassWithLongValueClass() {
		val binder = createDataBinder(LongValueClassRecord::class.java)
		binder.construct(TestValueResolver(mapOf("userId" to "1")))

		Assertions.assertThat(getTarget<LongValueClassRecord>(binder).userId).isEqualTo(UserId(1))
	}

	@Test
	fun constructDataClassWithUuidValueClass() {
		val uuid = UUID.randomUUID()
		val binder = createDataBinder(UuidValueClassRecord::class.java)
		binder.construct(TestValueResolver(mapOf("orderId" to uuid.toString())))

		Assertions.assertThat(getTarget<UuidValueClassRecord>(binder).orderId).isEqualTo(OrderId(uuid))
	}

	@Test
	fun constructDataClassWithNullablePrimitiveValueClass() {
		val binder = createDataBinder(NullablePrimitiveValueClassRecord::class.java)
		binder.construct(TestValueResolver(mapOf("userId" to "1")))

		Assertions.assertThat(getTarget<NullablePrimitiveValueClassRecord>(binder).userId).isEqualTo(NullableUserId(1))
	}

	@Test
	fun constructDataClassWithNestedValueClass() {
		val binder = createDataBinder(NestedValueClassRecord::class.java)
		binder.construct(TestValueResolver(mapOf("title" to "hello")))

		Assertions.assertThat(getTarget<NestedValueClassRecord>(binder).title).isEqualTo(NestedTitle(Title("hello")))
	}

	private fun createDataBinder(targetType: Class<*>): DataBinder {
		val binder = DataBinder(null)
		binder.targetType = ResolvableType.forClass(targetType)
		binder.conversionService = DefaultFormattingConversionService()
		return binder
	}

	private inline fun <reified T> getTarget(dataBinder: DataBinder): T {
		Assertions.assertThat(dataBinder.bindingResult.allErrors).isEmpty()
		return dataBinder.target as T
	}

	private data class StringValueClassRecord(val title: Title)

	@JvmInline
	value class Title(val value: String)

	private data class LongValueClassRecord(val userId: UserId)

	@JvmInline
	value class UserId(val value: Long)

	private data class UuidValueClassRecord(val orderId: OrderId)

	@JvmInline
	value class OrderId(val value: UUID)

	private data class NullablePrimitiveValueClassRecord(val userId: NullableUserId?)

	@JvmInline
	value class NullableUserId(val value: Long)

	private data class NestedValueClassRecord(val title: NestedTitle)

	@JvmInline
	value class NestedTitle(val value: Title)

	private class TestValueResolver(private val values: Map<String, Any>) : DataBinder.ValueResolver {

		override fun resolveValue(name: String, type: Class<*>): Any? = this.values[name]

		override fun getNames(): Set<String> = this.values.keys
	}
}
