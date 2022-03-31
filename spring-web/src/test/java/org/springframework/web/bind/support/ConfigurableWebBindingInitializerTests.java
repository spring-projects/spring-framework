/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.bind.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.bind.WebDataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConfigurableWebBindingInitializer}.
 *
 * @author Michaël van de Giessen
 */
public class ConfigurableWebBindingInitializerTests {
	private static final String[] ALLOWED = new String[]{"allowed"};
	private static final String[] DISALLOWED =  new String[]{"disallowed"};
	private WebDataBinder binder = new WebDataBinder(new TestBean());

	@Test
	void initBinderAllowedSet() {
		// if we set allowed / disallowed
		ConfigurableWebBindingInitializer sut = new ConfigurableWebBindingInitializer();
		sut.setAllowedFields(ALLOWED);
		sut.setDisallowedFields(DISALLOWED);
		sut.initBinder(this.binder);

		// we expect them to be set on the initialising WebDataBinder
		assertThat(this.binder.getAllowedFields()).containsExactly(ALLOWED);
		assertThat(this.binder.getDisallowedFields()).containsExactly(DISALLOWED);
	}

	@Test
	void initBinderAllowedNullIgnore() {
		this.binder.setAllowedFields(ALLOWED);
		this.binder.setDisallowedFields(DISALLOWED);

		// if we dont set allowed / disallowed (null = default)
		ConfigurableWebBindingInitializer sut = new ConfigurableWebBindingInitializer();
		sut.initBinder(this.binder);

		// we expect original values to be retained on the initialising WebDataBinder
		assertThat(this.binder.getAllowedFields()).containsExactly(ALLOWED);
		assertThat(this.binder.getDisallowedFields()).containsExactly(DISALLOWED);
	}
}
