/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEvent
import org.springframework.core.ResolvableType
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Kotlin tests for [ApplicationListenerMethodAdapter].
 *
 * @author Sebastien Deleuze
 */
class KotlinApplicationListenerMethodAdapterTests {

	private val sampleEvents = Mockito.spy(SampleEvents())

	@Test
	fun rawListener() {
		val method = ReflectionUtils.findMethod(SampleEvents::class.java, "handleRaw", ApplicationEvent::class.java, Continuation::class.java)!!
		supportsEventType(true, method, ResolvableType.forClass(ApplicationEvent::class.java))
	}

	@Test
	fun listenerWithMoreThanOneParameter() {
		val method = ReflectionUtils.findMethod(SampleEvents::class.java, "moreThanOneParameter",
			String::class.java, Int::class.java, Continuation::class.java)!!
		Assertions.assertThatIllegalStateException().isThrownBy {
			createTestInstance(
				method
			)
		}
	}

	private fun supportsEventType(match: Boolean, method: Method, eventType: ResolvableType) {
		val adapter: ApplicationListenerMethodAdapter = createTestInstance(method)
		Assertions.assertThat(adapter.supportsEventType(eventType))
			.`as`("Wrong match for event '$eventType' on $method").isEqualTo(match)
	}

	private fun createTestInstance(method: Method): ApplicationListenerMethodAdapter {
		return StaticApplicationListenerMethodAdapter(method, this.sampleEvents)
	}


	private class StaticApplicationListenerMethodAdapter(method: Method, private val targetBean: Any) :
		ApplicationListenerMethodAdapter("unused", targetBean.javaClass, method) {
			public override fun getTargetBean(): Any {
				return targetBean
			}
		}

	@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
	private class SampleEvents {

		@EventListener
		suspend fun handleRaw(event: ApplicationEvent) {
		}

		@EventListener
		suspend fun moreThanOneParameter(foo: String, bar: Int) {
		}
	}

}
