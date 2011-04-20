/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;
import org.springframework.web.util.UrlPathHelper;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

/**
 * Test fixture with {@link RequestMappingHandlerMethodMapping} testing its {@link RequestMappingKey} comparator.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestKeyComparatorTests {

	private RequestMappingHandlerMethodMapping handlerMapping;

	private MockHttpServletRequest request;

	@Before
	public void setup() {
		this.handlerMapping = new RequestMappingHandlerMethodMapping();
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void moreSpecificPatternWins() {
		request.setRequestURI("/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		Comparator<RequestMappingKey> comparator = handlerMapping.getMappingKeyComparator(lookupPath, request);
		RequestMappingKey key1 = new RequestMappingKey(asList("/fo*"), null);
		RequestMappingKey key2 = new RequestMappingKey(asList("/foo"), null);

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void equalPatterns() {
		request.setRequestURI("/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		Comparator<RequestMappingKey> comparator = handlerMapping.getMappingKeyComparator(lookupPath, request);
		RequestMappingKey key1 = new RequestMappingKey(asList("/foo*"), null);
		RequestMappingKey key2 = new RequestMappingKey(asList("/foo*"), null);

		assertEquals(0, comparator.compare(key1, key2));
	}

	@Test
	public void greaterNumberOfMatchingPatternsWins() throws Exception {
		request.setRequestURI("/foo.html");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		RequestMappingKey key1 = new RequestMappingKey(asList("/foo", "*.jpeg"), null);
		RequestMappingKey key2 = new RequestMappingKey(asList("/foo", "*.html"), null);
		RequestMappingKey match1 = handlerMapping.getMatchingMappingKey(key1, lookupPath, request);
		RequestMappingKey match2 = handlerMapping.getMatchingMappingKey(key2, lookupPath, request);
		List<RequestMappingKey> matches = asList(match1, match2);
		Collections.sort(matches, handlerMapping.getMappingKeyComparator(lookupPath, request));

		assertSame(match2.getPatterns(), matches.get(0).getPatterns());
	}

	@Test
	public void oneMethodWinsOverNone() {
		Comparator<RequestMappingKey> comparator = handlerMapping.getMappingKeyComparator("", request);
		RequestMappingKey key1 = new RequestMappingKey(null, null);
		RequestMappingKey key2 = new RequestMappingKey(null, asList(RequestMethod.GET));

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void methodsAndParams() {
		RequestMappingKey empty = new RequestMappingKey(null, null);
		RequestMappingKey oneMethod = new RequestMappingKey(null, asList(RequestMethod.GET));
		RequestMappingKey oneMethodOneParam =
				new RequestMappingKey(null, asList(RequestMethod.GET), RequestConditionFactory.parseParams("foo"), null, null);
		List<RequestMappingKey> list = asList(empty, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, handlerMapping.getMappingKeyComparator("", request));

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(empty, list.get(2));
	}

	@Test 
	@Ignore	// TODO : remove ignore
	public void acceptHeaders() {
		RequestMappingKey html = new RequestMappingKey(null, null, null, RequestConditionFactory.parseHeaders("accept=text/html"), null);
		RequestMappingKey xml = new RequestMappingKey(null, null, null, RequestConditionFactory.parseHeaders("accept=application/xml"), null);
		RequestMappingKey none = new RequestMappingKey(null, null);

		request.addHeader("Accept", "application/xml, text/html");
		Comparator<RequestMappingKey> comparator = handlerMapping.getMappingKeyComparator("", request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
		assertTrue(comparator.compare(xml, none) < 0);
		assertTrue(comparator.compare(none, xml) > 0);
		assertTrue(comparator.compare(html, none) < 0);
		assertTrue(comparator.compare(none, html) > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/*");
		comparator = handlerMapping.getMappingKeyComparator("", request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/pdf");
		comparator = handlerMapping.getMappingKeyComparator("", request);

		assertTrue(comparator.compare(html, xml) == 0);
		assertTrue(comparator.compare(xml, html) == 0);

		// See SPR-7000
		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html;q=0.9,application/xml");
		comparator = handlerMapping.getMappingKeyComparator("", request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
	}

}