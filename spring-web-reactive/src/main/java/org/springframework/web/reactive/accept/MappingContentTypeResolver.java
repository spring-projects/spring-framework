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

package org.springframework.web.reactive.accept;

import java.util.Set;

import org.springframework.http.MediaType;

/**
 * An extension of {@link RequestedContentTypeResolver} that maintains a mapping between
 * keys (e.g. file extension, query parameter) and media types.
 *
 * @author Rossen Stoyanchev
 */
public interface MappingContentTypeResolver extends RequestedContentTypeResolver {

	/**
	 * Resolve the given media type to a list of path extensions.
	 *
	 * @param mediaType the media type to resolve
	 * @return a list of extensions or an empty list, never {@code null}
	 */
	Set<String> getKeysFor(MediaType mediaType);

	/**
	 * Return all registered keys (e.g. "json", "xml").
	 * @return a list of keys or an empty list, never {@code null}
	 */
	Set<String> getKeys();

}
