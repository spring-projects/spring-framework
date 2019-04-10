/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * Strategy for matching on a {@link WebRequest}.
 *
 * @author Rob Winch
 * @since 4.2
 * @see org.springframework.test.web.servlet.htmlunit.HostRequestMatcher
 * @see org.springframework.test.web.servlet.htmlunit.UrlRegexRequestMatcher
 */
@FunctionalInterface
public interface WebRequestMatcher {

	/**
	 * Whether this matcher matches on the supplied web request.
	 * @param request the {@link WebRequest} to attempt to match on
	 * @return {@code true} if this matcher matches on the {@code WebRequest}
	 */
	boolean matches(WebRequest request);

}
