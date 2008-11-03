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

package org.springframework.web.servlet.tags.form;

import junit.framework.TestCase;

import javax.servlet.jsp.PageContext;

import org.springframework.mock.web.MockPageContext;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class TagIdGeneratorTests extends TestCase {

	public void testNextId() throws Exception {
		String name = "foo";
		PageContext pageContext = new MockPageContext();
		assertEquals("foo1", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo2", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo3", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo4", TagIdGenerator.nextId(name, pageContext));
		assertEquals("bar1", TagIdGenerator.nextId("bar", pageContext));
		pageContext = new MockPageContext();
		assertEquals("foo1", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo2", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo3", TagIdGenerator.nextId(name, pageContext));
		assertEquals("foo4", TagIdGenerator.nextId(name, pageContext));
		assertEquals("bar1", TagIdGenerator.nextId("bar", pageContext));
	}

}
