/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
