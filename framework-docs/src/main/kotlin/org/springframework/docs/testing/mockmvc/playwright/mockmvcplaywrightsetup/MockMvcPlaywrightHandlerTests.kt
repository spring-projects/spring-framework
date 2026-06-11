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

package org.springframework.docs.testing.mockmvc.playwright.mockmvcplaywrightsetup

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.playwright.MockMvcPlaywrightHandler
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringJUnitWebConfig
class MockMvcPlaywrightHandlerTests(wac: WebApplicationContext) {

	private lateinit var page: Page

	private val handler: MockMvcPlaywrightHandler

	// tag::init[]
	init {
		val mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
		handler = MockMvcPlaywrightHandler.builder(mockMvc).build()
	}
	// end::init[]

	// tag::setup[]
	@BeforeEach
	fun initPage() {
		page = browser.newPage()
		page.setDefaultTimeout(500.0)
		page.route({ url -> url.startsWith("http://localhost") }, handler)
		page.navigate("http://localhost/index.html")
	}

	// end::setup[]
	// tag::close[]
	@AfterEach
	fun closePage() {
		page.close()
	}
	// end::close[]

	companion object {

		private lateinit var playwright: Playwright
		private lateinit var browser: Browser

	// tag::init[]
	@BeforeAll
	@JvmStatic
	fun initPlaywright() {
		playwright = Playwright.create()
		browser = playwright.chromium().launch()
	}
	// end::init[]
	// tag::close[]
	@AfterAll
	@JvmStatic
	fun closePlaywright() {
		browser.close()
		playwright.close()
	}
	}

	// end::close[]
}
