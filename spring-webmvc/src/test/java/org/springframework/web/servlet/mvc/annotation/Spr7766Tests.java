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

package org.springframework.web.servlet.mvc.annotation;

import java.awt.Color;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

public class Spr7766Tests {

	@Test
	public void test() throws Exception {
		AnnotationMethodHandlerAdapter adapter = new AnnotationMethodHandlerAdapter();
		ConfigurableWebBindingInitializer binder = new ConfigurableWebBindingInitializer();
		GenericConversionService service = new DefaultConversionService();
		service.addConverter(new ColorConverter());
		binder.setConversionService(service);
		adapter.setWebBindingInitializer(binder);
		Spr7766Controller controller = new Spr7766Controller();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/colors");
		request.setPathInfo("/colors");
		request.addParameter("colors", "#ffffff,000000");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, controller);
	}

	public class ColorConverter implements Converter<String, Color> {
		@Override
		public Color convert(String source) { if (!source.startsWith("#")) source = "#" + source; return Color.decode(source); }
	}

}
