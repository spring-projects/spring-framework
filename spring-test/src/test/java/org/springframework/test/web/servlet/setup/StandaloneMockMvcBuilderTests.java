/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.setup;

import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class StandaloneMockMvcBuilderTests {


	// SPR-10825

	@Test
	public void placeHoldersInRequestMapping() throws Exception {

		TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
		builder.addPlaceHolderValue("sys.login.ajax", "/foo");
		builder.build();

		RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerExecutionChain chain = hm.getHandler(request);

		assertNotNull(chain);
		assertEquals("handleWithPlaceholders", ((HandlerMethod) chain.getHandler()).getMethod().getName());
	}


	@Controller
	private static class PlaceholderController {

		@RequestMapping(value = "${sys.login.ajax}")
		private void handleWithPlaceholders() { }
	}


	private static class TestStandaloneMockMvcBuilder extends StandaloneMockMvcBuilder {

		private WebApplicationContext wac;

		private TestStandaloneMockMvcBuilder(Object... controllers) {
			super(controllers);
		}

		@Override
		protected WebApplicationContext initWebAppContext() {
			this.wac = super.initWebAppContext();
			return this.wac;
		}
	}
}
