package org.springframework.mail/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.Test

import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions

/**
 * These tests are intended to verify correct behavior of SimpleMailMessage when used from
 * Kotlin with `-Xjsr305=strict` enabled to strictly enforce Java nullability annotations.
 *
 * Kotlin's JSR305 strict mode treats all non-annotated parameters and return types as
 * non-nullable, while Java code does not perform any such checks and can still pass and
 * capture null parameters and return values, respectively.
 *
 * Kotlin also treats null and definitely-non-null types as incompatible types. As such,
 * The get/set methods in [SimpleMailMessage] must therefore have coherent nullability
 * annotations for Kotlin to treat them as properties with getters and setters. Otherwise,
 * Kotlin sees them as functions with incompatible get/set types and does not associate
 * them.
 *
 * These errors often appear only at compile time and not in the IDE, making it somewhat
 * confusing to troubleshoot.
 *
 * __If any of the annotations on [SimpleMailMessage] are missing or incoherent with their
 * respective getter / setter method, this test will actually fail to compile, which is
 * a form of test failure in and of itself.__
 *
 * @author Steve Gerhardt
 */
class SimpleMailMessageJsr305ComplianceTests {

	private inline fun <reified T : Any> T.getUnsafeVarargSetterByReflection(name : String) : Function1<Array<Any?>?, Unit> {
		return { arrayForVararg ->
			this::class.declaredMemberFunctions
				.first {
					val isFirstValueParameterVararg = it.parameters
						.firstOrNull { p -> p.kind == KParameter.Kind.VALUE }
						?.isVararg == true

					it.name == name && isFirstValueParameterVararg
				}
				.call(this, arrayForVararg)
		}
	}

	@Test
	fun `Null message parameters via Java setters should be null via Kotlin getters`() {
		val message = SimpleMailMessage()
		message.setFrom(null)
		message.getUnsafeVarargSetterByReflection("setTo")(null)
		message.setReplyTo(null)
		message.getUnsafeVarargSetterByReflection("setCc")(null)
		message.getUnsafeVarargSetterByReflection("setBcc")(null)
		message.setSentDate(null)
		message.setSubject(null)
		message.setText(null)

		assert(message.from == null)
		assert(message.to == null)
		assert(message.replyTo == null)
		assert(message.cc == null)
		assert(message.bcc == null)
		assert(message.sentDate == null)
		assert(message.subject == null)
		assert(message.text == null)
	}

	@Test
	fun `To, CC, and BCC with lists of null recipients should appear as valid lists with null entries via Kotlin getters`() {
		val message = SimpleMailMessage()
		message.setTo(*arrayOf(null, null, null, null))
		message.setCc(*arrayOf(null, null, null))
		message.setBcc(*arrayOf(null, null))

		assert(message.to.let { it != null && it.size == 4 && it.all { item -> item == null } })
		assert(message.cc.let { it != null && it.size == 3 && it.all { item -> item == null } })
		assert(message.bcc.let { it != null && it.size == 2 && it.all { item -> item == null } })
	}

	@Test
	fun `Non-null message parameters via Java setters should be non-null via Kotlin getters`() {
		val message = SimpleMailMessage()
		message.setFrom("me@mail.org")
		message.setTo("you@mail.org")
		message.setReplyTo("reply@mail.org")
		message.setCc(*arrayOf("he@mail.org", "she@mail.org"))
		message.setBcc(*arrayOf("us@mail.org", "them@mail.org"))
		val sentDate = Date()
		message.setSentDate(sentDate)
		message.setSubject("my subject")
		message.setText("my text")

		assert(message.from == "me@mail.org")
		assert(message.to.let { it != null && it.contains("you@mail.org") })
		assert(message.replyTo == "reply@mail.org")
		assert(message.cc.let { it != null && it.toList() == listOf("he@mail.org", "she@mail.org") })
		assert(message.bcc.let { it != null && it.toList() == listOf("us@mail.org", "them@mail.org") })
		assert(message.sentDate == sentDate)
		assert(message.subject == "my subject")
		assert(message.text == "my text")
	}

	@Test
	fun `Message parameters can be set via Kotlin setters`() {
		val message = SimpleMailMessage()
		message.from = "me@mail.org"
		message.replyTo = "reply@mail.org"
		val sentDate = Date()
		message.sentDate = sentDate
		message.subject = "my subject"
		message.text = "my text"

		assert(message.from == "me@mail.org")
		assert(message.replyTo == "reply@mail.org")
		assert(message.sentDate == sentDate)
		assert(message.subject == "my subject")
		assert(message.text == "my text")
	}
}
