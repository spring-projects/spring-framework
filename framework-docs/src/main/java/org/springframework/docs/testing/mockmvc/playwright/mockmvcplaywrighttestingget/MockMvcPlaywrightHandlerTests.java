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

package org.springframework.docs.testing.mockmvc.playwright.mockmvcplaywrighttestingget;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringJUnitWebConfig
public class MockMvcPlaywrightHandlerTests {

	private Page page;

	// tag::test[]
	@Test
	public void testGetForm() {
		page.locator("#query").fill("spring");
		page.locator("#getForm button[type='submit']").click();

		assertThat(page.locator("#getResult")).hasText("query=spring");
	}
	// end::test[]

	// tag::controller[]
	@GetMapping(path = "/search", produces = MediaType.TEXT_PLAIN_VALUE)
	String search(@RequestParam String query) {
		return "query=" + query;
	}
	// end::controller[]
}
