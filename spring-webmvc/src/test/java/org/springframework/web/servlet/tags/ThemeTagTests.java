/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags;

import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.servlet.support.RequestContext;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 */
public class ThemeTagTests extends AbstractTagTests {

	@Test
	@SuppressWarnings("serial")
	public void themeTag() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		ThemeTag tag = new ThemeTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("themetest");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("theme test message", message.toString());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void requestContext() throws ServletException {
		PageContext pc = createPageContext();
		RequestContext rc = new RequestContext((HttpServletRequest) pc.getRequest());
		assertEquals("theme test message", rc.getThemeMessage("themetest"));
		assertEquals("theme test message", rc.getThemeMessage("themetest", (String[]) null));
		assertEquals("theme test message", rc.getThemeMessage("themetest", "default"));
		assertEquals("theme test message", rc.getThemeMessage("themetest", (Object[]) null, "default"));
		assertEquals("theme test message arg1",
				rc.getThemeMessage("themetestArgs", new String[] {"arg1"}));
		assertEquals("theme test message arg1",
				rc.getThemeMessage("themetestArgs", Arrays.asList(new String[] {"arg1"})));
		assertEquals("default", rc.getThemeMessage("themetesta", "default"));
		assertEquals("default", rc.getThemeMessage("themetesta", (List) null, "default"));
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] {"themetest"});
		assertEquals("theme test message", rc.getThemeMessage(resolvable));
	}

}
