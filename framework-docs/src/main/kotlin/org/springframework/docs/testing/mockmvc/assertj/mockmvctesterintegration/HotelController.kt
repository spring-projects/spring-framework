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

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterintegration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 *
 * @author Stephane Nicoll
 */
class HotelController {

	private val mockMvc = MockMvcTester.of(HotelController())


	fun perform() {
		// tag::perform[]
		// Static import on MockMvcRequestBuilders.get
		assertThat(mockMvc.perform(get("/hotels/{id}",42)))
			.hasStatusOk()
		// end::perform[]
	}

	fun performWithCustomMatcher() {
		// tag::perform[]
		// Static import on MockMvcResultMatchers.status
		assertThat(mockMvc.get().uri("/hotels/{id}", 42))
			.matches(status().isOk())
		// end::perform[]
	}

	class HotelController
}