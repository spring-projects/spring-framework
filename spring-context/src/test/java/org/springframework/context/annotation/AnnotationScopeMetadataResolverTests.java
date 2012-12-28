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

package org.springframework.context.annotation;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * @author Rick Evans
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public final class AnnotationScopeMetadataResolverTests {

	private AnnotationScopeMetadataResolver scopeMetadataResolver;


	@Before
	public void setUp() throws Exception {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver();
	}


	@Test
	public void testThatResolveScopeMetadataDoesNotApplyScopedProxyModeToASingleton() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithSingletonScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_SINGLETON, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.NO, scopeMetadata.getScopedProxyMode());
	}

	@Test
	public void testThatResolveScopeMetadataDoesApplyScopedProxyModeToAPrototype() {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(ScopedProxyMode.INTERFACES);
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithPrototypeScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_PROTOTYPE, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.INTERFACES, scopeMetadata.getScopedProxyMode());
	}

	@Test
	public void testThatResolveScopeMetadataDoesReadScopedProxyModeFromTheAnnotation() {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithScopedProxy.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals("request", scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.TARGET_CLASS, scopeMetadata.getScopedProxyMode());
	}

	@Test
	public void testCustomRequestScope() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithCustomRequestScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals("request", scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.NO, scopeMetadata.getScopedProxyMode());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorWithNullScopedProxyMode() {
		new AnnotationScopeMetadataResolver(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetScopeAnnotationTypeWithNullType() {
		scopeMetadataResolver.setScopeAnnotationType(null);
	}


	@Scope("singleton")
	private static final class AnnotatedWithSingletonScope {
	}


	@Scope("prototype")
	private static final class AnnotatedWithPrototypeScope {
	}


	@Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	private static final class AnnotatedWithScopedProxy {
	}


	@CustomRequestScope
	private static final class AnnotatedWithCustomRequestScope {
	}


	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Scope("request")
	public @interface CustomRequestScope {

	}

}
