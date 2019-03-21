/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.servlet.view.xslt;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class XsltViewResolverTests {

	@Test
	public void resolveView() throws Exception {
		StaticApplicationContext ctx = new StaticApplicationContext();

		String prefix = ClassUtils.classPackageAsResourcePath(getClass());
		String suffix = ".xsl";
		String viewName = "products";

		XsltViewResolver viewResolver = new XsltViewResolver();
		viewResolver.setPrefix(prefix);
		viewResolver.setSuffix(suffix);
		viewResolver.setApplicationContext(ctx);

		XsltView view = (XsltView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
		assertNotNull("View should not be null", view);
		assertEquals("Incorrect URL", prefix + viewName + suffix, view.getUrl());
	}
}
