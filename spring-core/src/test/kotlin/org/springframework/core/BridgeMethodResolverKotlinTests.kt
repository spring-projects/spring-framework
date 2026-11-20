/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [BridgeMethodResolver].
 *
 * @author Sebastien Deleuze
 */
class BridgeMethodResolverKotlinTests {

	@Test
	fun findBridgedMethod() {
		val unbridged = GenericRepository::class.java.getDeclaredMethod("delete", Int::class.java)
		val bridged = GenericRepository::class.java.getDeclaredMethod("delete", Any::class.java)
		assertThat(unbridged.isBridge).isFalse
		assertThat(bridged.isBridge).isTrue

		assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).`as`("Unbridged method not returned directly").isEqualTo(unbridged)
		assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).`as`("Incorrect bridged method returned").isEqualTo(unbridged)
	}

	@Test
	fun findBridgedMethodWithArrays() {
		val unbridged = GenericRepository::class.java.getDeclaredMethod("delete", Array<Int>::class.java)
		val bridged = GenericRepository::class.java.getDeclaredMethod("delete", Array<Any>::class.java)
		assertThat(unbridged.isBridge).isFalse
		assertThat(bridged.isBridge).isTrue

		assertThat(BridgeMethodResolver.findBridgedMethod(unbridged)).`as`("Unbridged method not returned directly").isEqualTo(unbridged)
		assertThat(BridgeMethodResolver.findBridgedMethod(bridged)).`as`("Incorrect bridged method returned").isEqualTo(unbridged)
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

