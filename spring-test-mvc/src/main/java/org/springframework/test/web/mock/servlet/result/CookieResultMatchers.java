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

package org.springframework.test.web.mock.servlet.result;

import static org.springframework.test.web.mock.AssertionErrors.assertEquals;
import static org.springframework.test.web.mock.AssertionErrors.assertTrue;

import javax.servlet.http.Cookie;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.test.web.mock.servlet.MvcResult;
import org.springframework.test.web.mock.servlet.ResultMatcher;

/**
 * Factory for response cookie assertions. An instance of this class is
 * typically accessed via {@link MockMvcResultMatchers#cookie()}.
 *
 * @author Rossen Stoyanchev
 * @author Thomas Bruyelle
 * @since 3.2
 */
public class CookieResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#cookie()}.
	 */
	protected CookieResultMatchers() {
	}

	/**
	 * Assert a cookie value with the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher value(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				Cookie cookie = result.getResponse().getCookie(name);
				assertTrue("Response cookie not found: " + name, cookie != null);
				MatcherAssert.assertThat("Response cookie", cookie.getValue(), matcher);
			}
		};
	}

	/**
	 * Assert a cookie value.
	 */
	public ResultMatcher value(String name, String value) {
		return value(name, Matchers.equalTo(value));
	}

	/**
	 * Assert a cookie exists. The existence check is irrespective of whether
	 * max age is 0 (i.e. expired).
	 */
	public ResultMatcher exists(final String name) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				Cookie cookie = result.getResponse().getCookie(name);
				assertTrue("No cookie with name: " + name, cookie != null);
			}
		};
	}

	/**
	 * Assert a cookie does not exist. Note that the existence check is
	 * irrespective of whether max age is 0, i.e. expired.
	 */
	public ResultMatcher doesNotExist(final String name) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				Cookie cookie = result.getResponse().getCookie(name);
				assertTrue("Unexpected cookie with name " + name, cookie == null);
			}
		};
	}

	/**
	 * Assert a cookie's maxAge with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher maxAge(final String name, final Matcher<? super Integer> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				Cookie cookie = result.getResponse().getCookie(name);
				assertTrue("No cookie with name: " + name, cookie != null);
				MatcherAssert.assertThat("Response cookie maxAge", cookie.getMaxAge(), matcher);
			}
		};
	}

	/**
	 * Assert a cookie's maxAge value.
	 */
	public ResultMatcher maxAge(String name, int maxAge) {
		return maxAge(name, Matchers.equalTo(maxAge));
	}

	/**
	 * Assert a cookie path with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher path(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				Cookie cookie = result.getResponse().getCookie(name);
				MatcherAssert.assertThat("Response cookie path", cookie.getPath(), matcher);
			}
		};
	}

	public ResultMatcher path(String name, String path) {
		return path(name, Matchers.equalTo(path));
	}

	/**
	 * Assert a cookie's domain with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher domain(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				Cookie cookie = result.getResponse().getCookie(name);
				MatcherAssert.assertThat("Response cookie domain", cookie.getDomain(), matcher);
			}
		};
	}

	/**
	 * Assert a cookie's domain value.
	 */
	public ResultMatcher domain(String name, String domain) {
		return domain(name, Matchers.equalTo(domain));
	}

	/**
	 * Assert a cookie's comment with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher comment(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				Cookie cookie = result.getResponse().getCookie(name);
				MatcherAssert.assertThat("Response cookie comment", cookie.getComment(), matcher);
			}
		};
	}

	/**
	 * Assert a cookie's comment value.
	 */
	public ResultMatcher comment(String name, String comment) {
		return comment(name, Matchers.equalTo(comment));
	}

	/**
	 * Assert a cookie's version with a Hamcrest {@link Matcher}
	 */
	public ResultMatcher version(final String name, final Matcher<? super Integer> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				Cookie cookie = result.getResponse().getCookie(name);
				MatcherAssert.assertThat("Response cookie version", cookie.getVersion(), matcher);
			}
		};
	}

	/**
	 * Assert a cookie's version value.
	 */
	public ResultMatcher version(String name, int version) {
		return version(name, Matchers.equalTo(version));
	}

	/**
	 * Assert whether the cookie must be sent over a secure protocol or not.
	 */
	public ResultMatcher secure(final String name, final boolean secure) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				Cookie cookie = result.getResponse().getCookie(name);
				assertEquals("Response cookie secure", secure, cookie.getSecure());
			}
		};
	}
}
