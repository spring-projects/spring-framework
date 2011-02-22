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
package org.springframework.web.servlet.config;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.easymock.Capture;
import org.junit.Test;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Feature;
import org.springframework.context.annotation.FeatureConfiguration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;

/**
 * Test fixture for {@link MvcInterceptors}.
 * @author Rossen Stoyanchev
 */
public class MvcInterceptorsTests {

	@Test
	public void testInterceptors() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MvcInterceptorsFeature.class);
		ctx.refresh();

		Iterator<MappedInterceptor> itr = ctx.getBeansOfType(MappedInterceptor.class).values().iterator();

		MappedInterceptor interceptor = itr.next();
		assertTrue(interceptor.getInterceptor() instanceof UserRoleAuthorizationInterceptor);
		assertNull(interceptor.getPathPatterns());

		interceptor = itr.next();
		assertTrue(interceptor.getInterceptor() instanceof LocaleChangeInterceptor);
		assertArrayEquals(new String[] { "/locale", "/locale/**" }, interceptor.getPathPatterns());

		interceptor = itr.next();
		assertTrue(interceptor.getInterceptor() instanceof ThemeChangeInterceptor);
		assertArrayEquals(new String[] { "/theme", "/theme/**" }, interceptor.getPathPatterns());

	}

	@Test
	public void validateNoInterceptors() {
		ProblemReporter reporter = createMock(ProblemReporter.class);
		Capture<Problem> captured = new Capture<Problem>();
		reporter.error(capture(captured));
		replay(reporter);

		boolean result = new MvcInterceptors().validate(reporter);

		assertFalse(result);
		assertEquals("No interceptors defined.", captured.getValue().getMessage());
	}

	@Test
	public void validateNullHandler() {
		ProblemReporter reporter = createMock(ProblemReporter.class);
		Capture<Problem> captured = new Capture<Problem>();
		reporter.error(capture(captured));
		replay(reporter);

		HandlerInterceptor[] interceptors = new HandlerInterceptor[] { null };
		boolean result = new MvcInterceptors().globalInterceptors(interceptors).validate(reporter);

		assertFalse(result);
		assertTrue(captured.getValue().getMessage().contains("Null interceptor"));
	}

	@Test
	public void validateEmptyPath() {
		ProblemReporter reporter = createMock(ProblemReporter.class);
		Capture<Problem> captured = new Capture<Problem>();
		reporter.error(capture(captured));
		replay(reporter);

		HandlerInterceptor[] interceptors = new HandlerInterceptor[] { new LocaleChangeInterceptor() };
		String[] patterns = new String[] { "" };
		boolean result = new MvcInterceptors().mappedInterceptors(patterns, interceptors).validate(reporter);

		assertFalse(result);
		assertTrue(captured.getValue().getMessage().startsWith("Empty path pattern specified for "));
	}

	@FeatureConfiguration
	private static class MvcInterceptorsFeature {

		@SuppressWarnings("unused")
		@Feature
		public MvcInterceptors interceptors() {
			return new MvcInterceptors()
				.globalInterceptors(new UserRoleAuthorizationInterceptor())
				.mappedInterceptors(new String[] { "/locale", "/locale/**" }, new LocaleChangeInterceptor())
				.mappedInterceptors(new String[] { "/theme", "/theme/**"},  new ThemeChangeInterceptor());
		}

	}

}
