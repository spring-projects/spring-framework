/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Abstract base class for {@link VersionStrategy} implementations that insert
 * a prefix into the URL path, for example, "version/static/myresource.js".
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
	public @Nullable String extractVersion(String requestPath) {
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
