/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for Kotlin support in [DefaultParameterNameDiscoverer].
 *
 * @author Sebastien Deleuze
 */
class DefaultParameterNameDiscovererKotlinTests :
	AbstractReflectionParameterNameDiscovererKotlinTests(DefaultParameterNameDiscoverer()){

	enum class MyEnum {
		ONE, TWO
	}

	@Test  // SPR-16931
	fun getParameterNamesOnEnum() {
		val constructor = MyEnum::class.java.declaredConstructors[0]
		val actualParams = parameterNameDiscoverer.getParameterNames(constructor)
		assertThat(actualParams).containsExactly("\$enum\$name", "\$enum\$ordinal")
	}

}
