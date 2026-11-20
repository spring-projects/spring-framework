/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import jakarta.servlet.jsp.PageContext;

/**
 * Utility class for generating '{@code id}' attributes values for JSP tags. Given the
 * name of a tag (the data bound path in most cases) returns a unique ID for that name within
 * the current {@link PageContext}. Each request for an ID for a given name will append an
 * ever-increasing counter to the name itself. For instance, given the name '{@code person.name}',
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
