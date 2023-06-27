/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link HttpServiceArgumentResolver} for arguments of type {@link MultipartFile}.
 * The argument is recognized by type, and does not need to be annotated. To make
 * it optional, declare the parameter with an {@link Optional} wrapper.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class MultipartFileArgumentResolver extends AbstractNamedValueArgumentResolver {

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Class<?> type = parameter.nestedIfOptional().getNestedParameterType();
		return (type.equals(MultipartFile.class) ?
				new NamedValueInfo("", true, null, "MultipartFile", true) : null);
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder values) {

		Assert.state(value instanceof MultipartFile, "Expected MultipartFile value");
		MultipartFile file = (MultipartFile) value;

		HttpHeaders headers = new HttpHeaders();
		if (file.getOriginalFilename() != null) {
			headers.setContentDispositionFormData(name, file.getOriginalFilename());
		}
		if (file.getContentType() != null) {
			headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());
		}

		values.addRequestPart(name, new HttpEntity<>(file.getResource(), headers));
	}

}
