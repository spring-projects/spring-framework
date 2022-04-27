/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class KotlinBridgeMethodResolverTests {

	@Test
	fun findBridgedMethod() {
		val unbridged = GenericRepository::class.java.getDeclaredMethod("delete", Int::class.java)
		val bridged = GenericRepository::class.java.getDeclaredMethod("delete", Any::class.java)
		Assertions.assertThat(unbridged.isBridge).isFalse
		Assertions.assertThat(bridged.isBridge).isTrue

		Assertions.assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).`as`("Unbridged method not returned directly").isEqualTo(unbridged)
		Assertions.assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).`as`("Incorrect bridged method returned").isEqualTo(unbridged)
	}

	@Test
	fun findBridgedMethodWithArrays() {
		val unbridged = GenericRepository::class.java.getDeclaredMethod("delete", Array<Int>::class.java)
		val bridged = GenericRepository::class.java.getDeclaredMethod("delete", Array<Any>::class.java)
		Assertions.assertThat(unbridged.isBridge).isFalse
		Assertions.assertThat(bridged.isBridge).isTrue

		Assertions.assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).`as`("Unbridged method not returned directly").isEqualTo(unbridged)
		Assertions.assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).`as`("Incorrect bridged method returned").isEqualTo(unbridged)
	}
}

interface GenericInterface<ID> {
	fun delete(id: ID)
	fun delete(ids: Array<ID>)
}

abstract class AbstractGenericClass<ID> : GenericInterface<ID> {

	override fun delete(id: ID) {
	}

	override fun delete(ids: Array<ID>) {
	}
}

class GenericRepository : AbstractGenericClass<Int>() {

	override fun delete(id: Int) {
		error("gotcha")
	}

	override fun delete(ids: Array<Int>) {
		error("gotcha")
	}
}

