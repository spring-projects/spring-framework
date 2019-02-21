/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Abstract base class for filename suffix based {@link VersionStrategy}
 * implementations, e.g. "static/myresource-version.js"
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractFileNameVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");


	@Override
	public String extractVersion(String requestPath) {
		Matcher matcher = pattern.matcher(requestPath);
		if (matcher.find()) {
			String match = matcher.group(1);
			return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
		}
		else {
			return null;
		}
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return StringUtils.delete(requestPath, "-" + version);
	}

	@Override
	public String addVersion(String requestPath, String version) {
		String baseFilename = StringUtils.stripFilenameExtension(requestPath);
		String extension = StringUtils.getFilenameExtension(requestPath);
		return (baseFilename + '-' + version + '.' + extension);
	}

}
