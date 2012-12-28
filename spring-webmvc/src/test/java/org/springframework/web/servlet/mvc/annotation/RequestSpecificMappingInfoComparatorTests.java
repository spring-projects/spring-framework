/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Arjen Poutsma
 */
public class RequestSpecificMappingInfoComparatorTests {

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator comparator;

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo emptyInfo;

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo oneMethodInfo;

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo twoMethodsInfo;

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo oneMethodOneParamInfo;

	private AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo oneMethodTwoParamsInfo;


	@Before
	public void setUp() throws NoSuchMethodException {
		comparator = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator(new MockComparator(), new MockHttpServletRequest());

		emptyInfo = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, new RequestMethod[0],null, null);

		oneMethodInfo = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, new RequestMethod[]{RequestMethod.GET}, null, null);

		twoMethodsInfo = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, new RequestMethod[]{RequestMethod.GET, RequestMethod.POST}, null, null);

		oneMethodOneParamInfo = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, new RequestMethod[]{RequestMethod.GET}, new String[]{"param"}, null);

		oneMethodTwoParamsInfo = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, new RequestMethod[]{RequestMethod.GET}, new String[]{"param1", "param2"}, null);
	}

	@Test
	public void sort() {
		List<AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo> infos = new ArrayList<AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo>();
		infos.add(emptyInfo);
		infos.add(oneMethodInfo);
		infos.add(twoMethodsInfo);
		infos.add(oneMethodOneParamInfo);
		infos.add(oneMethodTwoParamsInfo);

		Collections.shuffle(infos);
		Collections.sort(infos, comparator);

		assertEquals(oneMethodTwoParamsInfo, infos.get(0));
		assertEquals(oneMethodOneParamInfo, infos.get(1));
		assertEquals(oneMethodInfo, infos.get(2));
		assertEquals(twoMethodsInfo, infos.get(3));
		assertEquals(emptyInfo, infos.get(4));
	}

	@Test
	public void acceptHeaders() {
		AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo html = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, null, null, new String[] {"accept=text/html"});

		AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo xml = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, null, null, new String[] {"accept=application/xml"});

		AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo none = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo(null, null, null, null);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/html");
		comparator = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator(new MockComparator(), request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
		assertTrue(comparator.compare(xml, none) < 0);
		assertTrue(comparator.compare(none, xml) > 0);
		assertTrue(comparator.compare(html, none) < 0);
		assertTrue(comparator.compare(none, html) > 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/xml, text/*");
		comparator = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator(new MockComparator(), request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);

		request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/pdf");
		comparator = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator(new MockComparator(), request);

		assertTrue(comparator.compare(html, xml) == 0);
		assertTrue(comparator.compare(xml, html) == 0);

		// See SPR-7000
		request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html;q=0.9,application/xml");
		comparator = new AnnotationMethodHandlerAdapter.RequestSpecificMappingInfoComparator(new MockComparator(), request);

		assertTrue(comparator.compare(html, xml) > 0);
		assertTrue(comparator.compare(xml, html) < 0);
	}

	private static class MockComparator implements Comparator<String> {

		@Override
		public int compare(String s1, String s2) {
			return 0;
		}
	}

}
