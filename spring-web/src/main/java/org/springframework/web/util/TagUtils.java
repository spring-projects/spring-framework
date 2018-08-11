/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.Assert;

/**
 * Utility class for tag library related code, exposing functionality
 * such as translating {@link String Strings} to web scopes.
 *
 * <p>
 * <ul>
 * <li>{@code page} will be transformed to
 * {@link javax.servlet.jsp.PageContext#PAGE_SCOPE PageContext.PAGE_SCOPE}
 * <li>{@code request} will be transformed to
 * {@link javax.servlet.jsp.PageContext#REQUEST_SCOPE PageContext.REQUEST_SCOPE}
 * <li>{@code session} will be transformed to
 * {@link javax.servlet.jsp.PageContext#SESSION_SCOPE PageContext.SESSION_SCOPE}
 * <li>{@code application} will be transformed to
 * {@link javax.servlet.jsp.PageContext#APPLICATION_SCOPE PageContext.APPLICATION_SCOPE}
 * </ul>
 *
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public abstract class TagUtils {

	/** Constant identifying the page scope. */
	public static final String SCOPE_PAGE = "page";

	/** Constant identifying the request scope. */
	public static final String SCOPE_REQUEST = "request";

	/** Constant identifying the session scope. */
	public static final String SCOPE_SESSION = "session";

	/** Constant identifying the application scope. */
	public static final String SCOPE_APPLICATION = "application";


	/**
	 * Determines the scope for a given input {@code String}.
	 * <p>If the {@code String} does not match 'request', 'session',
	 * 'page' or 'application', the method will return {@link PageContext#PAGE_SCOPE}.
	 * @param scope the {@code String} to inspect
	 * @return the scope found, or {@link PageContext#PAGE_SCOPE} if no scope matched
	 * @throws IllegalArgumentException if the supplied {@code scope} is {@code null}
	 */
	public static int getScope(String scope) {
		Assert.notNull(scope, "Scope to search for cannot be null");
		if (scope.equals(SCOPE_REQUEST)) {
			return PageContext.REQUEST_SCOPE;
		}
		else if (scope.equals(SCOPE_SESSION)) {
			return PageContext.SESSION_SCOPE;
		}
		else if (scope.equals(SCOPE_APPLICATION)) {
			return PageContext.APPLICATION_SCOPE;
		}
		else {
			return PageContext.PAGE_SCOPE;
		}
	}

	/**
	 * Determine whether the supplied {@link Tag} has any ancestor tag
	 * of the supplied type.
	 * @param tag the tag whose ancestors are to be checked
	 * @param ancestorTagClass the ancestor {@link Class} being searched for
	 * @return {@code true} if the supplied {@link Tag} has any ancestor tag
	 * of the supplied type
	 * @throws IllegalArgumentException if either of the supplied arguments is {@code null};
	 * or if the supplied {@code ancestorTagClass} is not type-assignable to
	 * the {@link Tag} class
	 */
	public static boolean hasAncestorOfType(Tag tag, Class<?> ancestorTagClass) {
		Assert.notNull(tag, "Tag cannot be null");
		Assert.notNull(ancestorTagClass, "Ancestor tag class cannot be null");
		if (!Tag.class.isAssignableFrom(ancestorTagClass)) {
			throw new IllegalArgumentException(
					"Class '" + ancestorTagClass.getName() + "' is not a valid Tag type");
		}
		Tag ancestor = tag.getParent();
		while (ancestor != null) {
			if (ancestorTagClass.isAssignableFrom(ancestor.getClass())) {
				return true;
			}
			ancestor = ancestor.getParent();
		}
		return false;
	}

	/**
	 * Determine whether the supplied {@link Tag} has any ancestor tag
	 * of the supplied type, throwing an {@link IllegalStateException}
	 * if not.
	 * @param tag the tag whose ancestors are to be checked
	 * @param ancestorTagClass the ancestor {@link Class} being searched for
	 * @param tagName the name of the {@code tag}; for example '{@code option}'
	 * @param ancestorTagName the name of the ancestor {@code tag}; for example '{@code select}'
	 * @throws IllegalStateException if the supplied {@code tag} does not
	 * have a tag of the supplied {@code parentTagClass} as an ancestor
	 * @throws IllegalArgumentException if any of the supplied arguments is {@code null},
	 * or in the case of the {@link String}-typed arguments, is composed wholly
	 * of whitespace; or if the supplied {@code ancestorTagClass} is not
	 * type-assignable to the {@link Tag} class
	 * @see #hasAncestorOfType(javax.servlet.jsp.tagext.Tag, Class)
	 */
	public static void assertHasAncestorOfType(Tag tag, Class<?> ancestorTagClass, String tagName,
			String ancestorTagName) {

		Assert.hasText(tagName, "'tagName' must not be empty");
		Assert.hasText(ancestorTagName, "'ancestorTagName' must not be empty");
		if (!TagUtils.hasAncestorOfType(tag, ancestorTagClass)) {
			throw new IllegalStateException("The '" + tagName +
					"' tag can only be used inside a valid '" + ancestorTagName + "' tag.");
		}
	}

}
