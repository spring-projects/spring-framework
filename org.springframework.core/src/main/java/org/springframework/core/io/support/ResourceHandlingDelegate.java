/*
 * Copyright 2002-2009 the original author or authors.
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
import java.net.URL;
import java.net.URI;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.util.PathMatcher;

/**
 * Abstraction for a path matching strategy, for customizing the way in which a resource can be
 * scanned for finding underlying resources that match a given pattern.
 * Used for implementing application-server specific resource scanning strategies (e.g. for JBoss AS)
 *
 * @author Marius Bogoevici
 *
 */
public interface ResourceHandlingDelegate {

	boolean canHandleResource(URL url);

	boolean canHandleResource(URI uri);

	Set<Resource> findMatchingResources(Resource rootResource, String subPattern, PathMatcher pathMatcher) throws
			IOException;

	Resource loadResource(URL url) throws IOException;

	Resource loadResource(URI uri) throws IOException;

}
