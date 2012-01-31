/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.support.WebContentGenerator;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.junit.Assert.*;
import static org.junit.Assert.*;
import static org.junit.Assert.*;

/**
 * @author Rick Evans
 */
public class WebContentInterceptorTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		request = new MockHttpServletRequest();
		request.setMethod(WebContentGenerator.METHOD_GET);
		response = new MockHttpServletResponse();
	}


	@Test
	public void preHandleSetsCacheSecondsOnMatchingRequest() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);

		interceptor.preHandle(request, response, null);

		List expiresHeaders = response.getHeaders("Expires");
		assertNotNull("'Expires' header not set (must be) : null", expiresHeaders);
		assertTrue("'Expires' header not set (must be) : empty", expiresHeaders.size() > 0);
		List cacheControlHeaders = response.getHeaders("Cache-Control");
		assertNotNull("'Cache-Control' header not set (must be) : null", cacheControlHeaders);
		assertTrue("'Cache-Control' header not set (must be) : empty", cacheControlHeaders.size() > 0);
	}

	@Test
	public void preHandleSetsCacheSecondsOnMatchingRequestWithCustomCacheMapping() throws Exception {
		Properties mappings = new Properties();
		mappings.setProperty("**/*handle.vm", "-1");

		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(10);
		interceptor.setCacheMappings(mappings);
		
		request.setRequestURI("http://localhost:7070/example/adminhandle.vm");
		interceptor.preHandle(request, response, null);

		List expiresHeaders = response.getHeaders("Expires");
		assertSame("'Expires' header set (must not be) : empty", 0, expiresHeaders.size());
		List cacheControlHeaders = response.getHeaders("Cache-Control");
		assertSame("'Cache-Control' header set (must not be) : empty", 0, cacheControlHeaders.size());

		request.setRequestURI("http://localhost:7070/example/bingo.html");
		interceptor.preHandle(request, response, null);

		expiresHeaders = response.getHeaders("Expires");
		assertNotNull("'Expires' header not set (must be) : null", expiresHeaders);
		assertTrue("'Expires' header not set (must be) : empty", expiresHeaders.size() > 0);
		cacheControlHeaders = response.getHeaders("Cache-Control");
		assertNotNull("'Cache-Control' header not set (must be) : null", cacheControlHeaders);
		assertTrue("'Cache-Control' header not set (must be) : empty", cacheControlHeaders.size() > 0);
	}

	@Test
	public void preHandleSetsCacheSecondsOnMatchingRequestWithNoCaching() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(0);

		interceptor.preHandle(request, response, null);

		List expiresHeaders = response.getHeaders("Expires");
		assertNotNull("'Expires' header not set (must be) : null", expiresHeaders);
		assertTrue("'Expires' header not set (must be) : empty", expiresHeaders.size() > 0);
		List cacheControlHeaders = response.getHeaders("Cache-Control");
		assertNotNull("'Cache-Control' header not set (must be) : null", cacheControlHeaders);
		assertTrue("'Cache-Control' header not set (must be) : empty", cacheControlHeaders.size() > 0);
	}

	@Test
	public void preHandleSetsCacheSecondsOnMatchingRequestWithCachingDisabled() throws Exception {
		WebContentInterceptor interceptor = new WebContentInterceptor();
		interceptor.setCacheSeconds(-1);

		interceptor.preHandle(request, response, null);

		List expiresHeaders = response.getHeaders("Expires");
		assertSame("'Expires' header set (must not be) : empty", 0, expiresHeaders.size());
		List cacheControlHeaders = response.getHeaders("Cache-Control");
		assertSame("'Cache-Control' header set (must not be) : empty", 0, cacheControlHeaders.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPathMatcherToNull() throws Exception {
				WebContentInterceptor interceptor = new WebContentInterceptor();
				interceptor.setPathMatcher(null);
	}

}
