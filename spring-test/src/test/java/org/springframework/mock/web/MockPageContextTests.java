/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mock.web;

import jakarta.servlet.jsp.PageContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code MockPageContext} class.
 *
 * @author Rick Evans
 */
class MockPageContextTests {

	private final String key = "foo";

	private final String value = "bar";

	private final MockPageContext ctx = new MockPageContext();

	@Test
	void setAttributeWithNoScopeUsesPageScope() {
		ctx.setAttribute(key, value);
		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isEqualTo(value);
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

	@Test
	void removeAttributeWithNoScopeSpecifiedRemovesValueFromAllScopes() {
		ctx.setAttribute(key, value, PageContext.APPLICATION_SCOPE);
		ctx.removeAttribute(key);

		assertThat(ctx.getAttribute(key, PageContext.PAGE_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.APPLICATION_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.REQUEST_SCOPE)).isNull();
		assertThat(ctx.getAttribute(key, PageContext.SESSION_SCOPE)).isNull();
	}

}
