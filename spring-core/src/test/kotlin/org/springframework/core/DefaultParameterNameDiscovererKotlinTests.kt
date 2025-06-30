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
