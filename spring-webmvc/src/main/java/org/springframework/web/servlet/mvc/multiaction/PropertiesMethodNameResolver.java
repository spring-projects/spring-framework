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

package org.springframework.web.servlet.mvc.multiaction;

import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * The most flexible out-of-the-box implementation of the {@link MethodNameResolver}
 * interface. Uses {@code java.util.Properties} to define the mapping
 * between the URL of incoming requests and the corresponding method name.
 * Such properties can be held in an XML document.
 *
 * <p>Properties format is
 * {@code
 * /welcome.html=displayGenresPage
 * }
 * Note that method overloading isn't allowed, so there's no need to
 * specify arguments.
 *
 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
 * and a various Ant-style pattern matches, e.g. a registered "/t*" matches
 * both "/test" and "/team". For details, see the AntPathMatcher javadoc.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.util.Properties
 * @see org.springframework.util.AntPathMatcher
 * @deprecated as of 4.3, in favor of annotation-driven handler methods
 */
@Deprecated
public class PropertiesMethodNameResolver extends AbstractUrlMethodNameResolver
		implements InitializingBean {

	private Properties mappings;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * Set explicit URL to method name mappings through a Properties object.
	 * @param mappings Properties with URL as key and method name as value
	 */
	public void setMappings(Properties mappings) {
		this.mappings = mappings;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.mappings == null || this.mappings.isEmpty()) {
			throw new IllegalArgumentException("'mappings' property is required");
		}
	}


	@Override
	protected String getHandlerMethodNameForUrlPath(String urlPath) {
		String methodName = this.mappings.getProperty(urlPath);
		if (methodName != null) {
			return methodName;
		}
		Enumeration<?> propNames = this.mappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String registeredPath = (String) propNames.nextElement();
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return (String) this.mappings.get(registeredPath);
			}
		}
		return null;
	}

}
