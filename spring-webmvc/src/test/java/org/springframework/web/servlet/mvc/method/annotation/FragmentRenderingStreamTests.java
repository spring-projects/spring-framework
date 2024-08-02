/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for streaming of {@link ModelAndView} fragments.
 * @author Rossen Stoyanchev
 */
public class FragmentRenderingStreamTests {

	@Test
	void streamFragments() throws Exception {

		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		String prefix = "org/springframework/web/servlet/view/script/kotlin/";
		ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver(prefix, ".kts");
		viewResolver.setApplicationContext(context);

		ResponseBodyEmitterReturnValueHandler handler = new ResponseBodyEmitterReturnValueHandler(
				List.of(new MappingJackson2HttpMessageConverter()),
				ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(),
				new ContentNegotiationManager(),
				List.of(viewResolver), null);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		NativeWebRequest webRequest = new ServletWebRequest(request, response);

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(request, response);
		WebAsyncUtils.getAsyncManager(webRequest).setAsyncWebRequest(asyncWebRequest);
		request.setAsyncSupported(true);

		MethodParameter type = on(TestController.class).resolveReturnType(SseEmitter.class);
		SseEmitter emitter = new SseEmitter();
		handler.handleReturnValue(emitter, type, new ModelAndViewContainer(), webRequest);

		assertThat(request.isAsyncStarted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(200);

		ModelAndView mav1 = new ModelAndView("fragment1", Map.of("foo", "Foo"));
		ModelAndView mav2 = new ModelAndView("fragment2", Map.of("bar", "Bar"));

		emitter.send(SseEmitter.event().data(mav1).data(mav2));

		assertThat(response.getContentType()).isEqualTo("text/event-stream");
		assertThat(response.getContentAsString()).isEqualTo(("""
				data:<p>Hello Foo</p>
				data:<p>Hello Bar</p>

				"""));
	}


	private static class TestController {

		SseEmitter handle() {
			return null;
		}
	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		ScriptTemplateConfigurer kotlinScriptConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("kotlin");
			configurer.setScripts("org/springframework/web/servlet/view/script/kotlin/render.kts");
			configurer.setRenderFunction("render");
			return configurer;
		}

		@Bean
		ResourceBundleMessageSource messageSource() {
			ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
			messageSource.setBasename("org/springframework/web/servlet/view/script/messages");
			return messageSource;
		}
	}

}
