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

package org.springframework.web.method.support;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;

/**
 * Test fixture for {@link ModelAndViewContainer}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewContainerTests {

	private ModelAndViewContainer mavContainer;

	@Before
	public void setup() {
		this.mavContainer = new ModelAndViewContainer();
	}
	
	@Test
	public void getModel() {
		this.mavContainer.addAttribute("name", "value");
		assertEquals(1, this.mavContainer.getModel().size());
	}

	@Test
	public void getModelRedirectModel() {
		ModelMap redirectModel = new ModelMap("name", "redirectValue");
		this.mavContainer.setRedirectModel(redirectModel);
		this.mavContainer.addAttribute("name", "value");
		
		assertEquals("Default model should be used if not in redirect scenario",
				"value", this.mavContainer.getModel().get("name"));
	
		this.mavContainer.setRedirectModelScenario(true);
		
		assertEquals("Redirect model should be used in redirect scenario", 
				"redirectValue", this.mavContainer.getModel().get("name"));
	}

	@Test
	public void getModelIgnoreDefaultModelOnRedirect() {
		this.mavContainer.addAttribute("name", "value");
		this.mavContainer.setRedirectModelScenario(true);
		
		assertEquals("Default model should be used since no redirect model was provided", 
				1, this.mavContainer.getModel().size());

		this.mavContainer.setIgnoreDefaultModelOnRedirect(true);
		
		assertEquals("Empty model should be returned if no redirect model is available", 
				0, this.mavContainer.getModel().size());
	}

}
