/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Abstract base class for {@link VersionStrategy} implementations that insert
 * a prefix into the URL path, e.g. "version/static/myresource.js".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractPrefixVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());


	private final String prefix;


	protected AbstractPrefixVersionStrategy(String version) {
		Assert.hasText(version, "Version must not be empty");
		this.prefix = version;
	}


	@Override
	public String extractVersion(String requestPath) {
		return (requestPath.startsWith(this.prefix) ? this.prefix : null);
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return requestPath.substring(this.prefix.length());
	}

	@Override
	public String addVersion(String path, String version) {
		if (path.startsWith(".")) {
			return path;
		}
		else if (this.prefix.endsWith("/") || path.startsWith("/")) {
			return this.prefix + path;
		}
		else {
			return this.prefix + '/' + path;
		}
	}

}
