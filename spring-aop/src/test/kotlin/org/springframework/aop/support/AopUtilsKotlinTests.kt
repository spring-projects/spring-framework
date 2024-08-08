/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.support

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation

/**
 * Tests for Kotlin support in [AopUtils].
 *
 * @author Sebastien Deleuze
 */
class AopUtilsKotlinTests {

    @Test
    fun `Invoking suspending function should return Mono`() {
        val value = "foo"
        val method = ReflectionUtils.findMethod(AopUtilsKotlinTests::class.java, "suspendingFunction",
			String::class.java, Continuation::class.java)!!
        val continuation = Continuation<Any>(CoroutineName("test")) { }
		val result = AopUtils.invokeJoinpointUsingReflection(this, method, arrayOf(value, continuation))
        assertThat(result).isInstanceOfSatisfying(Mono::class.java) {
            assertThat(it.block()).isEqualTo(value)
        }
    }

    @Suppress("unused")
    suspend fun suspendingFunction(value: String): String {
        delay(1)
        return value
    }

}
