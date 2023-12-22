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

package org.springframework.scheduling.annotation

import org.aopalliance.intercept.MethodInvocation
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito

/**
 * Kotlin tests for [AnnotationAsyncExecutionInterceptor].
 *
 * @author Sebastien Deleuze
 */
class AnnotationAsyncExecutionInterceptorKotlinTests {

	@Test
	fun nullableUnitReturnValue() {
		val interceptor = AnnotationAsyncExecutionInterceptor(null)

		class C { @Async fun nullableUnit(): Unit? = null }
		val invocation = Mockito.mock<MethodInvocation>()
		given(invocation.method).willReturn(C::class.java.getDeclaredMethod("nullableUnit"))

		Assertions.assertThat(interceptor.invoke(invocation)).isNull()
	}

}
