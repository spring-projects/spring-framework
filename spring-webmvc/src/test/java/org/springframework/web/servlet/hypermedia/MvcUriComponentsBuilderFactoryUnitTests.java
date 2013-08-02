/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.web.servlet.hypermedia;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.hypermedia.MvcUriComponentsBuilderUnitTests.PersonControllerImpl;
import org.springframework.web.servlet.hypermedia.MvcUriComponentsBuilderUnitTests.PersonsAddressesController;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.web.servlet.hypermedia.MvcUriComponentsBuilder.*;

/**
 * Unit tests for {@link MvcUriComponentsBuilderFactory}.
 * 
 * @author Ricardo Gladwell
 * @author Oliver Gierke
 */
public class MvcUriComponentsBuilderFactoryUnitTests extends TestUtils {

	List<UriComponentsContributor> contributors = Collections.emptyList();

	MvcUriComponentsBuilderFactory factory = new MvcUriComponentsBuilderFactory(
			contributors);

	@Test
	public void createsLinkToControllerRoot() {

		URI link = factory.from(PersonControllerImpl.class).build().toUri();

		assertPointsToMockServer(link);
		assertThat(link.toString(), endsWith("/people"));
	}

	@Test
	public void createsLinkToParameterizedControllerRoot() {

		URI link = factory.from(PersonsAddressesController.class, 15).build().toUri();

		assertPointsToMockServer(link);
		assertThat(link.toString(), endsWith("/people/15/addresses"));
	}

	@Test
	public void appliesParameterValueIfContributorConfigured() {

		List<? extends UriComponentsContributor> contributors = Arrays.asList(new SampleUriComponentsContributor());
		MvcUriComponentsBuilderFactory factory = new MvcUriComponentsBuilderFactory(
				contributors);

		SpecialType specialType = new SpecialType();
		specialType.parameterValue = "value";

		URI link = factory.from(
				methodOn(SampleController.class).sampleMethod(1L, specialType)).build().toUri();
		assertPointsToMockServer(link);
		assertThat(link.toString(), endsWith("/sample/1?foo=value"));
	}

	/**
	 * @see #57
	 */
	@Test
	public void usesDateTimeFormatForUriBinding() {

		DateTime now = DateTime.now();

		MvcUriComponentsBuilderFactory factory = new MvcUriComponentsBuilderFactory(
				contributors);
		URI link = factory.from(methodOn(SampleController.class).sampleMethod(now)).build().toUri();
		assertThat(link.toString(),
				endsWith("/sample/" + ISODateTimeFormat.date().print(now)));
	}

	static interface SampleController {

		@RequestMapping("/sample/{id}")
		HttpEntity<?> sampleMethod(@PathVariable("id") Long id, SpecialType parameter);

		@RequestMapping("/sample/{time}")
		HttpEntity<?> sampleMethod(
				@PathVariable("time") @DateTimeFormat(iso = ISO.DATE) DateTime time);
	}

	static class SampleUriComponentsContributor implements UriComponentsContributor {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return SpecialType.class.equals(parameter.getParameterType());
		}

		@Override
		public void enhance(UriComponentsBuilder builder, MethodParameter parameter,
				Object value) {
			builder.queryParam("foo", ((SpecialType) value).parameterValue);
		}
	}

	static class SpecialType {

		String parameterValue;
	}
}
