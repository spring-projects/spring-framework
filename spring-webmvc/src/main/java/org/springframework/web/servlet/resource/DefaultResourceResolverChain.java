/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;


/**
 *
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	private static Log logger = LogFactory.getLog(DefaultResourceResolverChain.class);

	private final List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();

	private int index = -1;


	public DefaultResourceResolverChain(List<ResourceResolver> resolvers) {
		this.resolvers.addAll((resolvers != null) ? resolvers : new ArrayList<ResourceResolver>());
	}


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath, List<Resource> locations) {
		ResourceResolver resolver = getNextResolver();
		if (resolver == null) {
			return null;
		}
		try {
			logBefore(resolver);
			Resource resource = resolver.resolveResource(request, requestPath, locations, this);
			logAfter(resolver, resource);
			return resource;
		}
		finally {
			this.index--;
		}
	}

	@Override
	public String resolveUrlPath(String resourcePath, List<Resource> locations) {
		ResourceResolver resolver = getNextResolver();
		if (resolver == null) {
			return null;
		}
		try {
			logBefore(resolver);
			String urlPath = resolver.resolveUrlPath(resourcePath, locations, this);
			logAfter(resolver, urlPath);
			return urlPath;
		}
		finally {
			this.index--;
		}
	}

	private ResourceResolver getNextResolver() {

		Assert.state(this.index <= this.resolvers.size(),
				"Current index exceeds the number of configured ResourceResolver's");

		if (this.index == (this.resolvers.size() - 1)) {
			if (logger.isTraceEnabled()) {
				logger.trace("No more ResourceResolver's to delegate to, returning null");
			}
			return null;
		}

		this.index++;
		return this.resolvers.get(this.index);
	}

	private void logBefore(ResourceResolver resolver) {
		if (logger.isTraceEnabled()) {
			logger.trace("Calling " + resolver.getClass().getName() + " at index [" + this.index + "]");
		}
	}

	private void logAfter(ResourceResolver resolver, Object result) {
		if (logger.isTraceEnabled()) {
			logger.trace(resolver.getClass().getName() + " returned " + result);
		}
	}

}
