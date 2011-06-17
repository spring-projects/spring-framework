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

package org.springframework.web.servlet.mvc.method;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.RequestMethodsRequestCondition;

/**
 * Test fixture with {@link RequestMappingHandlerMapping} testing its {@link RequestMappingInfo} comparator.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoComparatorTests {

	private TestRequestMappingInfoHandlerMapping handlerMapping;

	private MockHttpServletRequest request;

	@Before
	public void setup() {
		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void moreSpecificPatternWins() {
		request.setRequestURI("/foo");
		Comparator<RequestMappingInfo> comparator = handlerMapping.getMappingComparator(request);
		RequestMappingInfo key1 = new RequestMappingInfo(new String[]{"/fo*"});
		RequestMappingInfo key2 = new RequestMappingInfo(new String[]{"/foo"});

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void equalPatterns() {
		request.setRequestURI("/foo");
		Comparator<RequestMappingInfo> comparator = handlerMapping.getMappingComparator(request);
		RequestMappingInfo key1 = new RequestMappingInfo(new String[]{"/foo*"});
		RequestMappingInfo key2 = new RequestMappingInfo(new String[]{"/foo*"});

		assertEquals(0, comparator.compare(key1, key2));
	}

	@Test
	public void greaterNumberOfMatchingPatternsWins() throws Exception {
		request.setRequestURI("/foo.html");
		RequestMappingInfo key1 = new RequestMappingInfo(new String[]{"/foo", "*.jpeg"});
		RequestMappingInfo key2 = new RequestMappingInfo(new String[]{"/foo", "*.html"});
		RequestMappingInfo match1 = handlerMapping.getMatchingMapping(key1, request);
		RequestMappingInfo match2 = handlerMapping.getMatchingMapping(key2, request);
		List<RequestMappingInfo> matches = asList(match1, match2);
		Collections.sort(matches, handlerMapping.getMappingComparator(request));

		assertSame(match2.getPatternsCondition(), matches.get(0).getPatternsCondition());
	}

	@Test
	public void oneMethodWinsOverNone() {
		Comparator<RequestMappingInfo> comparator = handlerMapping.getMappingComparator(request);
		RequestMappingInfo key1 = new RequestMappingInfo(null);
		RequestMappingInfo key2 = new RequestMappingInfo(null, new RequestMethod[] {RequestMethod.GET});

		assertEquals(1, comparator.compare(key1, key2));
	}

	@Test
	public void methodsAndParams() {
		RequestMappingInfo empty = new RequestMappingInfo(null);
		RequestMappingInfo oneMethod = new RequestMappingInfo(null, new RequestMethod[] {RequestMethod.GET});
		RequestMappingInfo oneMethodOneParam =
				new RequestMappingInfo(null, new RequestMethodsRequestCondition(RequestMethod.GET), 
						new ParamsRequestCondition("foo"), null, null, null);
		List<RequestMappingInfo> list = asList(empty, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, handlerMapping.getMappingComparator(request));

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(empty, list.get(2));
	}

	@Test
	public void produces() {
		RequestMappingInfo html = new RequestMappingInfo(null, null, null, null, null, new ProducesRequestCondition("text/html"));
		RequestMappingInfo xml = new RequestMappingInfo(null, null, null, null, null, new ProducesRequestCondition("application/xml"));
		RequestMappingInfo none = new RequestMappingInfo(null);

		request.addHeader("Accept", "application/xml, text/html");
		Comparator<RequestMappingInfo> comparator = handlerMapping.getMappingComparator(request);

		int result = comparator.compare(html, xml);
		assertTrue("Invalid comparison result: " + result, result > 0);
		assertTrue(comparator.compare(xml, html) < 0);
		assertTrue(comparator.compare(xml, none) < 0);
		assertTrue(comparator.compare(none, xml) > 0);
		assertTrue(comparator.compare(html, none) < 0);
		assertTrue(comparator.compare(none, html) > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/*");
		comparator = handlerMapping.getMappingComparator(request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/pdf");
		comparator = handlerMapping.getMappingComparator(request);

		assertTrue(comparator.compare(html, xml) == 0);
		assertTrue(comparator.compare(xml, html) == 0);

		// See SPR-7000
		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html;q=0.9,application/xml");
		comparator = handlerMapping.getMappingComparator(request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
	}
	
	private static class TestRequestMappingInfoHandlerMapping extends RequestMappingInfoHandlerMapping {

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return false;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			return null;
		}
	}

}