/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.servlet.jsp.PageContext;

/**
 * Utility class for generating '{@code id}' attributes values for JSP tags. Given the
 * name of a tag (the data bound path in most cases) returns a unique ID for that name within
 * the current {@link PageContext}. Each request for an ID for a given name will append an
 * ever increasing counter to the name itself. For instance, given the name '{@code person.name}',
 * the first request will give '{@code person.name1}' and the second will give
 * '{@code person.name2}'. This supports the common use case where a set of radio or check buttons
 * are generated for the same data field, with each button being a distinct tag instance.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class TagIdGenerator {

	/**
	 * The prefix for all {@link PageContext} attributes created by this tag.
	 */
	private static final String PAGE_CONTEXT_ATTRIBUTE_PREFIX = TagIdGenerator.class.getName() + ".";

	/**
	 * Get the next unique ID (within the given {@link PageContext}) for the supplied name.
	 */
	public static String nextId(String name, PageContext pageContext) {
		String attributeName = PAGE_CONTEXT_ATTRIBUTE_PREFIX + name;
		Integer currentCount = (Integer) pageContext.getAttribute(attributeName);
		currentCount = (currentCount != null ? currentCount + 1 : 1);
		pageContext.setAttribute(attributeName, currentCount);
		return (name + currentCount);
	}

}
