/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterassertionsjson

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ThrowingConsumer
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 *
 * @author Stephane Nicoll
 */
class FamilyControllerTests {

	private val mockMvc = MockMvcTester.of(FamilyController())


	fun extractingPathAsMap() {
		// tag::extract-asmap[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
			.extractingPath("$.members[0]")
			.asMap()
			.contains(entry("name", "Homer"))
		// end::extract-asmap[]
	}

	fun extractingPathAndConvertWithType() {
		// tag::extract-convert[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
			.extractingPath("$.members[0]")
			.convertTo(Member::class.java)
			.satisfies(ThrowingConsumer { member: Member ->
				assertThat(member.name).isEqualTo("Homer")
			})
		// end::extract-convert[]
	}

	fun extractingPathAndConvertWithAssertFactory() {
		// tag::extract-convert-assert-factory[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
			.extractingPath("$.members")
			.convertTo(InstanceOfAssertFactories.list(Member::class.java))
			.hasSize(5)
			.element(0).satisfies(ThrowingConsumer { member: Member ->
				assertThat(member.name).isEqualTo("Homer")
			})
		// end::extract-convert-assert-factory[]
	}

	fun assertTheSimpsons() {
		// tag::assert-file[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
			.isStrictlyEqualTo("sample/simpsons.json")
		// end::assert-file[]
	}

	class FamilyController

	@JvmRecord
	data class Member(val name: String)
}