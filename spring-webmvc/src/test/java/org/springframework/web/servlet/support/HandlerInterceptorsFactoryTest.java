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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsArray.*;
import static org.junit.Assert.*;

/** @author Tadaya Tsuyukubo */
public class HandlerInterceptorsFactoryTest {

	@Test
	public void testCreateInstance() throws Exception {

		HandlerInterceptor fooInterceptor = createMock(HandlerInterceptor.class);
		HandlerInterceptor barInterceptor = createMock(HandlerInterceptor.class);
		HandlerInterceptor bazInterceptor = createMock(HandlerInterceptor.class);

		// list of handler interceptors: [foo,bar,baz]
		List<HandlerInterceptor> handlerInterceptors = new ArrayList<HandlerInterceptor>();
		handlerInterceptors.add(fooInterceptor);
		handlerInterceptors.add(barInterceptor);
		handlerInterceptors.add(bazInterceptor);

		// create additional mapped interceptors: [foo,bar[
		MappedInterceptor fooAdditional = new MappedInterceptor(new String[]{"/add/foo"}, fooInterceptor);
		MappedInterceptor barAdditional = new MappedInterceptor(new String[]{"/add/bar"}, barInterceptor);
		List<MappedInterceptor> additionalMappedInterceptors = new ArrayList<MappedInterceptor>();
		additionalMappedInterceptors.add(fooAdditional);
		additionalMappedInterceptors.add(barAdditional);

		// create interceptorsByPath: "/foo" => [foo], "/foobar" => [foo,bar]
		List<HandlerInterceptor> fooHandlers = new ArrayList<HandlerInterceptor>();
		fooHandlers.add(fooInterceptor);
		List<HandlerInterceptor> fooBarHandlers = new ArrayList<HandlerInterceptor>();
		fooBarHandlers.add(fooInterceptor);
		fooBarHandlers.add(barInterceptor);
		Map<String, List<HandlerInterceptor>> interceptorsByPath = new HashMap<String, List<HandlerInterceptor>>();
		interceptorsByPath.put("/foo", fooHandlers); // one interceptor
		interceptorsByPath.put("/foobar", fooBarHandlers); // two interceptors

		// prepare factory
		HandlerInterceptorsFactory factory = new HandlerInterceptorsFactory();
		factory.setHandlerInterceptors(handlerInterceptors);
		factory.setAdditionalMappedInterceptors(additionalMappedInterceptors);
		factory.setInterceptorsByPath(interceptorsByPath);
		List<Object> interceptors = factory.createInstance();

		// expected: [foo,bar,baz, mapped-foo => ["/add/foo"], mapped-bar => ["/add/bar"],
		//           mapped-foo => ["/foo","/foobar"], mapped-bar => ["/foobar"]
		assertThat("expected total size", interceptors.size(), is(7));

		// verify normal handler interceptors
		List<HandlerInterceptor> actualHandlerInterceptors = filterByClass(interceptors, HandlerInterceptor.class);
		assertThat(actualHandlerInterceptors, hasItems(fooInterceptor, barInterceptor, bazInterceptor));
		assertThat(actualHandlerInterceptors.size(), is(3));

		// verify additional mapped interceptors
		List<MappedInterceptor> actualMappedInterceptors = filterByClass(interceptors, MappedInterceptor.class);
		assertThat(actualMappedInterceptors, hasItems(fooAdditional, barAdditional));
		assertThat("expected num of MappedHandler", actualMappedInterceptors.size(), is(4));

		// remove the additional mapped interceptors
		actualMappedInterceptors.remove(fooAdditional);
		actualMappedInterceptors.remove(barAdditional);

		// sort by size of path patterns
		Collections.sort(actualMappedInterceptors, new MappedInterceptorPatternSizeComparator());

		// verify MappedInterceptors mapped by url path

		// foo interceptor
		MappedInterceptor firstMappedInterceptor = actualMappedInterceptors.get(0);
		assertThat(firstMappedInterceptor.getInterceptor(), sameInstance(fooInterceptor));
		assertThat(firstMappedInterceptor.getPathPatterns(), hasItemInArray("/foo"));
		assertThat(firstMappedInterceptor.getPathPatterns(), hasItemInArray("/foobar"));
		assertThat(firstMappedInterceptor.getPathPatterns().length, is(2));

		// bar interceptor
		MappedInterceptor secondMappedInterceptor = actualMappedInterceptors.get(1);
		assertThat(secondMappedInterceptor.getInterceptor(), sameInstance(barInterceptor));
		assertThat(secondMappedInterceptor.getPathPatterns(), array(is("/foobar")));

	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> filterByClass(List<Object> interceptors, Class<T> clazz) {
		// wish i could use google-guava
		List<T> result = new ArrayList<T>();
		for (Object interceptor : interceptors) {
			if (clazz.isAssignableFrom(interceptor.getClass())) {
				result.add((T) interceptor);
			}
		}
		return result;
	}

	private static class MappedInterceptorPatternSizeComparator implements Comparator<MappedInterceptor> {

		public int compare(MappedInterceptor left, MappedInterceptor right) {
			int leftSize = left.getPathPatterns().length;
			int rightSize = right.getPathPatterns().length;
			if (leftSize == rightSize) {
				return 0;
			}
			return leftSize < rightSize ? 1 : -1;
		}
	}

}
