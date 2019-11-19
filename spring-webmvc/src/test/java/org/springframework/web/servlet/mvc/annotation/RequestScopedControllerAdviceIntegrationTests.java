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

package org.springframework.web.servlet.mvc.annotation;

import java.util.List;

import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for request-scoped {@link ControllerAdvice @ControllerAdvice} beans.
 *
 * @author Sam Brannen
 * @since 5.1.12
 */
public class RequestScopedControllerAdviceIntegrationTests {

	@Test // gh-23985
	public void loadContextWithRequestScopedControllerAdvice() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(Config.class);
		context.refresh();

		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(context);
		assertEquals(1, adviceBeans.size());

		ControllerAdviceBean adviceBean = adviceBeans.get(0);
		assertEquals(RequestScopedControllerAdvice.class, adviceBean.getBeanType());
		assertEquals(42, adviceBean.getOrder());

		context.close();
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		@RequestScope
		RequestScopedControllerAdvice requestScopedControllerAdvice() {
			return new RequestScopedControllerAdvice();
		}
	}

	@ControllerAdvice
	@Order(42)
	static class RequestScopedControllerAdvice implements Ordered {

		@Override
		public int getOrder() {
			return 99;
		}
	}

}
