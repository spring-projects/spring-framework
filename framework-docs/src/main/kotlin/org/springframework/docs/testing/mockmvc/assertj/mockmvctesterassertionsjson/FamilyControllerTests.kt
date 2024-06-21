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