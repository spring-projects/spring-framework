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

package org.springframework.web.servlet.mvc.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Arjen Poutsma
 */
public class RequestMappingInfoComparatorTests {

	private AnnotationMethodHandlerAdapter.RequestMappingInfoComparator comparator;

	private AnnotationMethodHandlerAdapter.RequestMappingInfo emptyInfo;

	private AnnotationMethodHandlerAdapter.RequestMappingInfo oneMethodInfo;

	private AnnotationMethodHandlerAdapter.RequestMappingInfo twoMethodsInfo;

	private AnnotationMethodHandlerAdapter.RequestMappingInfo oneMethodOneParamInfo;

	private AnnotationMethodHandlerAdapter.RequestMappingInfo oneMethodTwoParamsInfo;


	@Before
	public void setUp() throws NoSuchMethodException {
		comparator = new AnnotationMethodHandlerAdapter.RequestMappingInfoComparator(new MockComparator());

		emptyInfo = new AnnotationMethodHandlerAdapter.RequestMappingInfo();

		oneMethodInfo = new AnnotationMethodHandlerAdapter.RequestMappingInfo();
		oneMethodInfo.methods = new RequestMethod[]{RequestMethod.GET};

		twoMethodsInfo = new AnnotationMethodHandlerAdapter.RequestMappingInfo();
		twoMethodsInfo.methods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};

		oneMethodOneParamInfo = new AnnotationMethodHandlerAdapter.RequestMappingInfo();
		oneMethodOneParamInfo.methods = new RequestMethod[]{RequestMethod.GET};
		oneMethodOneParamInfo.params = new String[]{"param"};

		oneMethodTwoParamsInfo = new AnnotationMethodHandlerAdapter.RequestMappingInfo();
		oneMethodTwoParamsInfo.methods = new RequestMethod[]{RequestMethod.GET};
		oneMethodTwoParamsInfo.params = new String[]{"param1", "param2"};
	}

	@Test
	public void sort() {
		List<AnnotationMethodHandlerAdapter.RequestMappingInfo> infos = new ArrayList<AnnotationMethodHandlerAdapter.RequestMappingInfo>();
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


	private static class MockComparator implements Comparator<String> {

		public int compare(String s1, String s2) {
			return 0;
		}
	}

}
