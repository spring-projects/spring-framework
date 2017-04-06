/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.reactive.result.view;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;

/**
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Render {

	Optional<Object> view();

	Map<String, Object> model();

	Optional<HttpStatus> status();

	HttpHeaders headers();


	static Builder<?> view(String name) {
		return null;
	}


	interface Builder<B extends Builder<B>> {

		B modelAttribute(String name, Object value);

		B modelMap(Map<String, ?> model);

		B model(Model model);

		B status(HttpStatus status);

		B header(String headerName, String... headerValues);

		B headers(HttpHeaders headers);

		Render build();

	}

}
