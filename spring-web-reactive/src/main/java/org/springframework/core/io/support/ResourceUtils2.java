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

package org.springframework.core.io.support;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * @author Arjen Poutsma
 */
public abstract class ResourceUtils2 {

	/**
	 * Indicates whether the given resource has a file, so that {@link
	 * Resource#getFile()}
	 * can be called without an {@link java.io.IOException}.
	 * @param resource the resource to check
	 * @return {@code true} if the given resource has a file; {@code false} otherwise
	 */
	// TODO: refactor into Resource.hasFile() method
	public static boolean hasFile(Resource resource) {
		Assert.notNull(resource, "'resource' must not be null");

		// the following Resource implementations do not support getURI/getFile
		if (resource instanceof ByteArrayResource ||
				resource instanceof DescriptiveResource ||
				resource instanceof InputStreamResource) {
			return false;
		}
		try {
			URI resourceUri = resource.getURI();
			return ResourceUtils.URL_PROTOCOL_FILE.equals(resourceUri.getScheme());
		}
		catch (IOException ignored) {
		}
		return false;
	}
}
