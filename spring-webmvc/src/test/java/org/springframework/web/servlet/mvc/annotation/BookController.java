/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Used for testing the combination of ControllerClassNameHandlerMapping/SimpleUrlHandlerMapping with @RequestParam in
 * {@link ServletAnnotationControllerTests}. Implemented as a top-level class (rather than an inner class) to make the
 * ControllerClassNameHandlerMapping work.
 *
 * @author Arjen Poutsma
 */
@Controller
public class BookController {

	@RequestMapping("list")
	public void list(Writer writer) throws IOException {
		writer.write("list");
	}

	@RequestMapping("show")
	public void show(@RequestParam(required = true) Long id, Writer writer) throws IOException {
		writer.write("show-id=" + id);
	}

	@RequestMapping(method = RequestMethod.POST)
	public void create(Writer writer) throws IOException {
		writer.write("create");
	}

}
