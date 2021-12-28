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

package org.springframework.web.servlet.tags;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.servlet.support.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).isEqualTo("theme test message");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void requestContext() throws ServletException {
		PageContext pc = createPageContext();
		RequestContext rc = new RequestContext((HttpServletRequest) pc.getRequest());
		assertThat(rc.getThemeMessage("themetest")).isEqualTo("theme test message");
		assertThat(rc.getThemeMessage("themetest", (String[]) null)).isEqualTo("theme test message");
		assertThat(rc.getThemeMessage("themetest", "default")).isEqualTo("theme test message");
		assertThat(rc.getThemeMessage("themetest", (Object[]) null, "default")).isEqualTo("theme test message");
		assertThat(rc.getThemeMessage("themetestArgs", new String[]{"arg1"})).isEqualTo("theme test message arg1");
		assertThat(rc.getThemeMessage("themetestArgs", Arrays.asList(new String[]{"arg1"}))).isEqualTo("theme test message arg1");
		assertThat(rc.getThemeMessage("themetesta", "default")).isEqualTo("default");
		assertThat(rc.getThemeMessage("themetesta", (List) null, "default")).isEqualTo("default");
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] {"themetest"});
		assertThat(rc.getThemeMessage(resolvable)).isEqualTo("theme test message");
	}

}
