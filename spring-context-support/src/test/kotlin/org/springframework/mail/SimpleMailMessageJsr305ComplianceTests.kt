/*
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

package org.springframework.mail

import org.junit.jupiter.api.Test

import java.util.Date

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
@Suppress("UsePropertyAccessSyntax")
class SimpleMailMessageJsr305ComplianceTests {

	/**
	 * Allows Kotlin code to pass a singular `null` to a `vararg`-accepting Java method.
	 * Normally, Kotlin does not allow null to be passed as a `vararg` parameter as it
	 * internally interprets the type signature as Array<T>, which is not nullable.
	 *
	 * However, Java allows (in theory) for null to be passed as a single vararg,
	 * rendering the array nullable. But it is not possible for Kotlin code to exercise
	 * that code path directly, hence this helper function.
	 *
	 * This function wraps the original function having the signature `vararg param : P`
	 * in Kotlin or `P... param` in Java, and allows it to accept Array<[P]?>? instead.
	 *
	 * @param P The type of the `vararg` parameter of the function to be called. Should
	 * 	not need to be passed explicitly.
	 * @param varargFunctionReference A function reference to the vararg instance method
	 * 	to be called.
	 * @return A function that accepts `null` or an array of [P] elements
	 */
	private inline fun <reified T : Any, reified P> unsafeVarargSetterByReflection(
		noinline varargFunctionReference : T.(Array<P>) -> Unit,
	) : T.(Array<P>?) -> Unit {
		return { arrayForVararg ->
			// Must make call via reflection instead of directly to avoid type checks
			// on the array parameter.
			@Suppress("UNCHECKED_CAST")
			(varargFunctionReference as kotlin.reflect.KFunction1<T, Array<P>?>)
				.call(this, arrayForVararg)
		}
	}

	private fun SimpleMailMessage.unsafeSetTo(varargParams : Array<String>?) =
		unsafeVarargSetterByReflection(SimpleMailMessage::setTo)(varargParams)

	private fun SimpleMailMessage.unsafeSetCc(varargParams : Array<String>?) =
		unsafeVarargSetterByReflection(SimpleMailMessage::setCc)(varargParams)

	private fun SimpleMailMessage.unsafeSetBcc(varargParams : Array<String>?) =
		unsafeVarargSetterByReflection(SimpleMailMessage::setBcc)(varargParams)

	// Warning suppressed intentionally - avoid calling Kotlin getters or setters for here
	// since we are ensuring the helper method works correctly.
	@Test
	@Suppress("UsePropertyAccessSyntax")
	fun `Unsafe vararg setter should successfully call a vararg-accepting setter method`() {
		val message = SimpleMailMessage()

		message.unsafeSetTo(arrayOf("test@example.com"))
		assert(message.getTo()?.toList() == listOf("test@example.com"))
		message.unsafeSetTo(null)
		assert(message.getTo() == null)

		message.unsafeSetCc(arrayOf("test2@example.com"))
		assert(message.getCc()?.toList() == listOf("test2@example.com"))
		message.unsafeSetCc(null)
		assert(message.getCc() == null)

		message.unsafeSetBcc(arrayOf("test3@example.com"))
		assert(message.getBcc()?.toList() == listOf("test3@example.com"))
		message.unsafeSetBcc(null)
		assert(message.getBcc() == null)
	}

	// Suppressed because the intent is to directly call the setter method - the JSR305
	// annotations being missing will still allow this test to succeed but only if it
	// calls the setter methods directly.
	@Test
	@Suppress("UsePropertyAccessSyntax")
	fun `Null message parameters via Java setters should be null via Kotlin getters`() {
		val message = SimpleMailMessage()
		message.setFrom(null)
		message.unsafeSetTo(null)
		message.setReplyTo(null)
		message.unsafeSetTo(null)
		message.unsafeSetTo(null)
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
	@Suppress("UsePropertyAccessSyntax")
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
		assert(message.to?.toList() == listOf("you@mail.org"))
		assert(message.replyTo == "reply@mail.org")
		assert(message.cc?.toList() == listOf("he@mail.org", "she@mail.org"))
		assert(message.bcc?.toList() == listOf("us@mail.org", "them@mail.org"))
		assert(message.sentDate == sentDate)
		assert(message.subject == "my subject")
		assert(message.text == "my text")
	}

	// If this test prevents successful compilation, it is nearly guaranteed the nullability
	// annotation is missing on the setter method for the erroneous line of code.
	@Test
	fun `Message parameters can be set via Kotlin setters`() {
		val message = SimpleMailMessage()
		message.from = "me@mail.org"
		message.setTo("mail1@mail.org", "mail2@mail.org")
		message.replyTo = "reply@mail.org"
		message.setCc("mail3@mail.org", "mail4@mail.org")
		message.setBcc("mail5@mail.org")
		val sentDate = Date()
		message.sentDate = sentDate
		message.subject = "my subject"
		message.text = "my text"

		assert(message.from == "me@mail.org")
		assert(message.to?.toList() == listOf("mail1@mail.org", "mail2@mail.org"))
		assert(message.replyTo == "reply@mail.org")
		assert(message.cc?.toList() == listOf("mail3@mail.org", "mail4@mail.org"))
		assert(message.bcc?.toList() == listOf("mail5@mail.org"))
		assert(message.sentDate == sentDate)
		assert(message.subject == "my subject")
		assert(message.text == "my text")
	}

	@Test
	fun `Message parameters can be set to null values via Kotlin setters`() {
		val message = SimpleMailMessage()
		message.from = null
		message.unsafeSetTo(null)
		message.replyTo = null
		message.unsafeSetCc(null)
		message.unsafeSetBcc(null)
		message.sentDate = null
		message.subject = null
		message.text = null

		assert(message.from == null)
		assert(message.to == null)
		assert(message.replyTo == null)
		assert(message.cc == null)
		assert(message.bcc == null)
		assert(message.sentDate == null)
		assert(message.subject == null)
		assert(message.text == null)
	}
}
