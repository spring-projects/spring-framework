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

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterassertionsjson;

import org.assertj.core.api.InstanceOfAssertFactories;

import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class FamilyControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new FamilyController());


	void extractingPathAsMap() {
		// tag::extract-asmap[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
				.extractingPath("$.members[0]")
				.asMap()
				.contains(entry("name", "Homer"));
		// end::extract-asmap[]
	}

	void extractingPathAndConvertWithType() {
		// tag::extract-convert[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
				.extractingPath("$.members[0]")
				.convertTo(Member.class)
				.satisfies(member -> assertThat(member.name).isEqualTo("Homer"));
		// end::extract-convert[]
	}

	void extractingPathAndConvertWithAssertFactory() {
		// tag::extract-convert-assert-factory[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
				.extractingPath("$.members")
				.convertTo(InstanceOfAssertFactories.list(Member.class))
				.hasSize(5)
				.element(0).satisfies(member -> assertThat(member.name).isEqualTo("Homer"));
		// end::extract-convert-assert-factory[]
	}

	void assertTheSimpsons() {
		// tag::assert-file[]
		assertThat(mockMvc.get().uri("/family")).bodyJson()
				.isStrictlyEqualTo("sample/simpsons.json");
		// end::assert-file[]
	}

	static class FamilyController {}

	record Member(String name) {}
}
