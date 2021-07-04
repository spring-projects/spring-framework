/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.method.annotation;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with
 * {@link org.springframework.web.method.annotation.MapMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class MapMethodProcessorTests {

	private MapMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private final ResolvableMethod resolvable =
			ResolvableMethod.on(getClass()).annotPresent(RequestMapping.class).build();


	@BeforeEach
	public void setUp() throws Exception {
		this.processor = new MapMethodProcessor();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void supportsParameter() {
		assertThat(this.processor.supportsParameter(
				this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class))).isTrue();
		assertThat(this.processor.supportsParameter(
				this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class))).isFalse();
	}

	@Test
	public void supportsReturnType() {
		assertThat(this.processor.supportsReturnType(this.resolvable.returnType())).isTrue();
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		MethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class);
		assertThat(this.processor.resolveArgument(param, this.mavContainer, this.webRequest, null)).isSameAs(this.mavContainer.getModel());
	}

	@Test
	public void handleMapReturnValue() throws Exception {
		this.mavContainer.addAttribute("attr1", "value1");
		Map<String, Object> returnValue = new ModelMap("attr2", "value2");

		this.processor.handleReturnValue(
				returnValue , this.resolvable.returnType(), this.mavContainer, this.webRequest);

		assertThat(mavContainer.getModel().get("attr1")).isEqualTo("value1");
		assertThat(mavContainer.getModel().get("attr2")).isEqualTo("value2");
	}


	@SuppressWarnings("unused")
	@RequestMapping
	private Map<String, Object> handle(
			Map<String, Object> map,
			@RequestBody Map<String, Object> annotMap) {

		return null;
	}

}
