/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import junit.framework.TestCase;

import javax.servlet.jsp.PageContext;

/**
 * Unit tests for the {@code MockPageContext} class.
 *
 * @author Rick Evans
 */
public final class MockPageContextTests extends TestCase {

	public void testSetAttributeWithNoScopeUsesPageScope() throws Exception {
		String key = "foo";
		String value = "bar";

		MockPageContext ctx = new MockPageContext();
		ctx.setAttribute(key, value);
		assertEquals(value, ctx.getAttribute(key, PageContext.PAGE_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.REQUEST_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.SESSION_SCOPE));
	}

	public void testRemoveAttributeWithNoScopeSpecifiedRemovesValueFromAllScopes() throws Exception {
		String key = "foo";
		String value = "bar";

		MockPageContext ctx = new MockPageContext();
		ctx.setAttribute(key, value, PageContext.APPLICATION_SCOPE);
		ctx.removeAttribute(key);

		assertNull(ctx.getAttribute(key, PageContext.PAGE_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.REQUEST_SCOPE));
		assertNull(ctx.getAttribute(key, PageContext.SESSION_SCOPE));
	}

}
