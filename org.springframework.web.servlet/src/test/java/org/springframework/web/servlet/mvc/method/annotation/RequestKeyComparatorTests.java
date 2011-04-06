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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestConditionFactory;
import org.springframework.web.servlet.mvc.method.annotation.RequestKey;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodMapping;

/**
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
		Comparator<RequestKey> comparator = handlerMapping.getKeyComparator(request);
		RequestKey key1 = new RequestKey(asList("/fo*"), null, null, null);
		RequestKey key2 = new RequestKey(asList("/foo"), null, null, null);

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void equalPatterns() {
		request.setRequestURI("/foo");
		Comparator<RequestKey> comparator = handlerMapping.getKeyComparator(request);
		RequestKey key1 = new RequestKey(asList("/foo*"), null, null, null);
		RequestKey key2 = new RequestKey(asList("/foo*"), null, null, null);

		assertEquals(0, comparator.compare(key1, key2));
	}

	@Test
	public void greaterNumberOfMatchingPatternsWins() throws Exception {
		request.setRequestURI("/foo.html");
		RequestKey key1 = new RequestKey(asList("/foo", "*.jpeg"), null, null, null);
		RequestKey key2 = new RequestKey(asList("/foo", "*.html"), null, null, null);
		RequestKey match1 = handlerMapping.getMatchingKey(key1, request);
		RequestKey match2 = handlerMapping.getMatchingKey(key2, request);
		List<RequestKey> matches = asList(match1, match2);
		Collections.sort(matches, handlerMapping.getKeyComparator(request));

		assertSame(match2.getPatterns(), matches.get(0).getPatterns());
	}

	@Test
	public void oneMethodWinsOverNone() {
		Comparator<RequestKey> comparator = handlerMapping.getKeyComparator(request);
		RequestKey key1 = new RequestKey(null, null, null, null);
		RequestKey key2 = new RequestKey(null, asList(RequestMethod.GET), null, null);

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void methodsAndParams() {
		RequestKey empty = new RequestKey(null, null, null, null);
		RequestKey oneMethod = new RequestKey(null, asList(RequestMethod.GET), null, null);
		RequestKey oneMethodOneParam =
				new RequestKey(null, asList(RequestMethod.GET), RequestConditionFactory.parseParams("foo"), null);
		List<RequestKey> list = asList(empty, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, handlerMapping.getKeyComparator(request));

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(empty, list.get(2));
	}

	@Test 
	@Ignore	// TODO : remove ignore
	public void acceptHeaders() {
		RequestKey html = new RequestKey(null, null, null, RequestConditionFactory.parseHeaders("accept=text/html"));
		RequestKey xml = new RequestKey(null, null, null, RequestConditionFactory.parseHeaders("accept=application/xml"));
		RequestKey none = new RequestKey(null, null, null, null);

		request.addHeader("Accept", "application/xml, text/html");
		Comparator<RequestKey> comparator = handlerMapping.getKeyComparator(request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
		assertTrue(comparator.compare(xml, none) < 0);
		assertTrue(comparator.compare(none, xml) > 0);
		assertTrue(comparator.compare(html, none) < 0);
		assertTrue(comparator.compare(none, html) > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/*");
		comparator = handlerMapping.getKeyComparator(request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/pdf");
		comparator = handlerMapping.getKeyComparator(request);

		assertTrue(comparator.compare(html, xml) == 0);
		assertTrue(comparator.compare(xml, html) == 0);

		// See SPR-7000
		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html;q=0.9,application/xml");
		comparator = handlerMapping.getKeyComparator(request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
	}
}
