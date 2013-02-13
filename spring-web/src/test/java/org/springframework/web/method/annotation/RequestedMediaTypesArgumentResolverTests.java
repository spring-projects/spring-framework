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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;


/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestedMediaTypesArgumentResolver}.
 * @author Arnaud Cogoluègnes
 */
public class RequestedMediaTypesArgumentResolverTests {

	private RequestedMediaTypesArgumentResolver resolver;
	
	private ContentNegotiationStrategy contentNegotiationStrategy;

	@Before
	public void setUp() throws Exception {
		contentNegotiationStrategy = Mockito.mock(ContentNegotiationStrategy.class);
		resolver = new RequestedMediaTypesArgumentResolver(contentNegotiationStrategy);
	}
	
	@Test public void supportParametersCollectionOfMediaTypes() throws Exception {
		MethodParameter methodParameter = methodParameter("collectionOfMediaTypes", Collection.class);
		Assert.assertTrue(resolver.supportsParameter(methodParameter));
	}
	
	@Test public void supportParametersCollectionOfStrings() throws Exception {
		MethodParameter methodParameter = methodParameter("collectionOfStrings", Collection.class);
		Assert.assertFalse(resolver.supportsParameter(methodParameter));
	}
	
	@Test public void supportParametersListOfMediaTypes() throws Exception {
		MethodParameter methodParameter = methodParameter("listOfMediaTypes", List.class);
		Assert.assertTrue(resolver.supportsParameter(methodParameter));
	}
	
	@Test public void supportParametersListOfStrings() throws Exception {
		MethodParameter methodParameter = methodParameter("listOfStrings", List.class);
		Assert.assertFalse(resolver.supportsParameter(methodParameter));
	}
	
	@SuppressWarnings("unchecked")
	@Test public void resolveArgument() throws Exception {
		Mockito.when(contentNegotiationStrategy.resolveMediaTypes(Mockito.any(NativeWebRequest.class)))
			.thenReturn(Arrays.asList(MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML));
		
		List<MediaType> mediaTypes = (List<MediaType>) resolver.resolveArgument(null, null, null, null);
		Assert.assertEquals(2,mediaTypes.size());
	}
	
	@SuppressWarnings("unchecked")
	@Test public void resolveArgumentEmptyNegociation() throws Exception {
		Mockito.when(contentNegotiationStrategy.resolveMediaTypes(Mockito.any(NativeWebRequest.class)))
			.thenReturn(new ArrayList<MediaType>());
		
		List<MediaType> mediaTypes = (List<MediaType>) resolver.resolveArgument(null, null, null, null);
		Assert.assertEquals(1,mediaTypes.size());
		Assert.assertEquals(MediaType.ALL,mediaTypes.get(0));
		
	}
	
	private MethodParameter methodParameter(String methodName,Class<?> paramClass) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName, paramClass);
		MethodParameter methodParam = new MethodParameter(method,0);
		return methodParam;
	}
	
	
	@SuppressWarnings("unused")
	private void collectionOfMediaTypes(Collection<MediaType> mediaTypes) { }
	
	@SuppressWarnings("unused")
	private void collectionOfStrings(Collection<String> mediaTypes) { }
	
	@SuppressWarnings("unused")
	private void listOfMediaTypes(List<MediaType> mediaTypes) { }
	
	@SuppressWarnings("unused")
	private void listOfStrings(List<String> mediaTypes) { }
	
}
