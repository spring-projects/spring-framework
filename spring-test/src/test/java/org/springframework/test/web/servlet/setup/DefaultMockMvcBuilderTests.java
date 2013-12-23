/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.test.web.servlet.setup;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tests for {@link DefaultMockMvcBuilder}.
 *
 * @author Rob Winch
 */
public class DefaultMockMvcBuilderTests {

	private StandaloneMockMvcBuilder builder;

	@Before
	public void setup() {
		builder = MockMvcBuilders.standaloneSetup(new PersonController());
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFiltersFiltersNull() {
		builder.addFilters((Filter[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFiltersFiltersContainsNull() {
		builder.addFilters(new ContinueFilter(), (Filter) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFilterPatternsNull() {
		builder.addFilter(new ContinueFilter(), (String[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFilterPatternContainsNull() {
		builder.addFilter(new ContinueFilter(), (String) null);
	}


	@Controller
	private static class PersonController {
		@RequestMapping(value="/forward")
		public String forward() {
			return "forward:/persons";
		}
	}

	private class ContinueFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {
			filterChain.doFilter(request, response);
		}
	}

}
