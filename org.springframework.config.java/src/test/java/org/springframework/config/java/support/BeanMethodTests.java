/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.config.java.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.config.java.StandardScopes.*;
import static org.springframework.config.java.support.MutableAnnotationUtils.*;
import static org.springframework.context.annotation.ScopedProxyMode.*;

import java.lang.reflect.Modifier;

import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.config.java.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;


/**
 * Unit tests for {@link BeanMethod}.
 *
 * @author Chris Beams
 */
public class BeanMethodTests {
	
	private ProblemReporter problemReporter = new FailFastProblemReporter();
	private String beanName = "foo";
	private Bean beanAnno = createMutableAnnotation(Bean.class);
	private ModelClass returnType = new ModelClass("FooType");
	private ConfigurationClass declaringClass = new ConfigurationClass();
	{ declaringClass.setName("test.Config"); }

	@Test
	public void testWellFormedMethod() {
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno);

		assertThat(beanMethod.getName(), sameInstance(beanName));
		assertThat(beanMethod.getModifiers(), equalTo(0));
		assertThat(beanMethod.getReturnType(), sameInstance(returnType));
		assertThat(beanMethod.getAnnotation(Bean.class), sameInstance(beanAnno));
		assertThat(beanMethod.getAnnotation(Override.class), nullValue());
		assertThat(beanMethod.getRequiredAnnotation(Bean.class), sameInstance(beanAnno));
		try {
			beanMethod.getRequiredAnnotation(Override.class);
			fail("expected IllegalArgumentException ex");
		} catch (IllegalArgumentException ex) { /* expected */ }

		// must call setDeclaringClass() before calling getLocation()
		try {
			beanMethod.getLocation();
			fail("expected IllegalStateException ex");
		} catch (IllegalStateException ex) { /* expected */ }


		beanMethod.setDeclaringClass(declaringClass);
		assertThat(beanMethod.getDeclaringClass(), sameInstance(declaringClass));

		beanMethod.setSource(12); // indicating a line number
		assertEquals(beanMethod.getSource(), 12);

		Location location = beanMethod.getLocation();
		assertEquals(location.getResource(), new ClassPathResource("test/Config"));
		assertEquals(location.getSource(), 12);

		// should validate without throwing as this is a well-formed method
		beanMethod.validate(problemReporter);
	}

	@Test
	public void finalMethodsAreIllegal() {
		BeanMethod beanMethod = new BeanMethod(beanName, Modifier.FINAL, returnType, beanAnno);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to final bean method");
		} catch (BeanDefinitionParsingException ex) {
			assertTrue(ex.getMessage().contains("remove the final modifier"));
		}
	}

	@Test
	public void privateMethodsAreIllegal() {
		BeanMethod beanMethod = new BeanMethod(beanName, Modifier.PRIVATE, returnType, beanAnno);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to private bean method");
		} catch (BeanDefinitionParsingException ex) {
			assertTrue(ex.getMessage().contains("increase the method's visibility"));
		}
	}

	@Test
	public void singletonInterfaceScopedProxiesAreIllegal() {
		Scope scope = SingletonInterfaceProxy.class.getAnnotation(Scope.class);
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno, scope);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to singleton with scoped proxy");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("cannot be created for singleton/prototype beans"));
		}
	}

	@Test
	public void singletonTargetClassScopedProxiesAreIllegal() {
		Scope scope = SingletonTargetClassProxy.class.getAnnotation(Scope.class);
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno, scope);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to singleton with scoped proxy");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("cannot be created for singleton/prototype beans"));
		}
	}

	@Test
	public void singletonsSansProxyAreLegal() {
		Scope scope = SingletonNoProxy.class.getAnnotation(Scope.class);
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno, scope);
		beanMethod.setDeclaringClass(declaringClass);
		beanMethod.validate(problemReporter); // should validate without problems - it's legal
	}

	@Test
	public void prototypeInterfaceScopedProxiesAreIllegal() {
		Scope scope = PrototypeInterfaceProxy.class.getAnnotation(Scope.class);
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno, scope);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to prototype with scoped proxy");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("cannot be created for singleton/prototype beans"));
		}
	}

	@Test
	public void sessionInterfaceScopedProxiesAreLegal() {
		Scope scope = PrototypeInterfaceProxy.class.getAnnotation(Scope.class);
		BeanMethod beanMethod = new BeanMethod(beanName, 0, returnType, beanAnno, scope);
		beanMethod.setDeclaringClass(declaringClass);
		try {
			beanMethod.validate(problemReporter);
			fail("should have failed due to prototype with scoped proxy");
		} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("cannot be created for singleton/prototype beans"));
		}
	}

	@Scope(value=SINGLETON, proxyMode=INTERFACES)
	private class SingletonInterfaceProxy { }

	@Scope(value=SINGLETON, proxyMode=TARGET_CLASS)
	private class SingletonTargetClassProxy { }

	@Scope(value=SINGLETON, proxyMode=NO)
	private class SingletonNoProxy { }

	@Scope(value=PROTOTYPE, proxyMode=INTERFACES)
	private class PrototypeInterfaceProxy { }

	@Scope(value=SESSION, proxyMode=INTERFACES)
	private class SessionInterfaceProxy { }
}
