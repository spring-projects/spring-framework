/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for [KotlinReflectionParameterNameDiscoverer].
 *
 * @author Sebastien Deleuze
 */
class KotlinReflectionParameterNameDiscovererTests :
	AbstractReflectionParameterNameDiscovererKotlinTests(KotlinReflectionParameterNameDiscoverer()) {

	@Test
	fun getParameterNamesOnZeroArgConstructor() {
		val constructor = ZeroArgClass::class.java.getDeclaredConstructor()
		assertThat(parameterNameDiscoverer.getParameterNames(constructor)).isEmpty()
	}

	@Test
	fun getParameterNamesOnInnerClassConstructor() {
		val constructor = InnerClass::class.java.declaredConstructors[0]
		assertThat(parameterNameDiscoverer.getParameterNames(constructor)).containsExactly("label")
	}

	class ZeroArgClass

	inner class InnerClass(val label: String)

}
