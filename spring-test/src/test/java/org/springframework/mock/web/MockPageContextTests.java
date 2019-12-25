/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.mock.web;

import javax.servlet.jsp.PageContext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@code MockPageContext} class.
 *
 * @author Rick Evans
 */
class MockPageContextTests {

	private final String key = "foo";

	private final String value = "bar";

	private final MockPageContext ctx = new MockPageContext();

	@Test
	void setAttributeWithNoScopeUsesPageScope() throws Exception {
		ctx.setAttribute(key, value);
		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isEqualTo(value);
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

	@Test
	void removeAttributeWithNoScopeSpecifiedRemovesValueFromAllScopes() throws Exception {
		ctx.setAttribute(key, value, PageContext.APPLICATION_SCOPE);
		ctx.removeAttribute(key);

		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

}
