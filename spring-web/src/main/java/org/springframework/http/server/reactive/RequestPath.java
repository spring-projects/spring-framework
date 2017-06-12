/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.http.server.reactive;

/**
 * Represents the complete path for a request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestPath extends PathSegmentContainer {

	/**
	 * Returns the portion of the URL path that represents the application.
	 * The context path is always at the beginning of the path and starts but
	 * does not end with "/". It is shared for URLs of the same application.
	 * <p>The context path may come from the underlying runtime API such as
	 * when deploying as a WAR to a Servlet container or it may also be assigned
	 * through the use of {@link ContextPathCompositeHandler} or both.
	 */
	PathSegmentContainer contextPath();

	/**
	 * The portion of the request path after the context path.
	 */
	PathSegmentContainer pathWithinApplication();

}
