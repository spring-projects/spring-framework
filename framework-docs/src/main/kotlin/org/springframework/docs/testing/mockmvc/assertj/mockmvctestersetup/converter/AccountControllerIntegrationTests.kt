/*
 * Copyright 2002-2025 the original author or authors.
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

@file:Suppress("DEPRECATION")

package org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup.converter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup.ApplicationWebConfiguration
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.web.context.WebApplicationContext

// tag::snippet[]
@SpringJUnitWebConfig(ApplicationWebConfiguration::class)
class AccountControllerIntegrationTests(@Autowired wac: WebApplicationContext) {

	private val mockMvc = MockMvcTester.from(wac).withHttpMessageConverters(
		listOf(wac.getBean(AbstractJackson2HttpMessageConverter::class.java)))

	// ...

}
// end::snippet[]

