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

package org.springframework.aop.aspectj;

import org.junit.Test;

import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer.AmbiguousBindingException;

/**
 * Tests just the annotation binding part of {@link AspectJAdviceParameterNameDiscoverer};
 * see supertype for remaining tests.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public final class TigerAspectJAdviceParameterNameDiscovererTests
		extends AspectJAdviceParameterNameDiscovererTests {

	@Test
	public void testAtThis() {
		assertParameterNames(getMethod("oneAnnotation"),"@this(a)",new String[]{"a"});
	}

	@Test
	public void testAtTarget() {
		assertParameterNames(getMethod("oneAnnotation"),"@target(a)",new String[]{"a"});
	}

	@Test
	public void testAtArgs() {
		assertParameterNames(getMethod("oneAnnotation"),"@args(a)",new String[]{"a"});
	}

	@Test
	public void testAtWithin() {
		assertParameterNames(getMethod("oneAnnotation"),"@within(a)",new String[]{"a"});
	}

	@Test
	public void testAtWithincode() {
		assertParameterNames(getMethod("oneAnnotation"),"@withincode(a)",new String[]{"a"});
	}

	@Test
	public void testAtAnnotation() {
		assertParameterNames(getMethod("oneAnnotation"),"@annotation(a)",new String[]{"a"});
	}

	@Test
	public void testAmbiguousAnnotationTwoVars() {
		assertException(getMethod("twoAnnotations"),"@annotation(a) && @this(x)",AmbiguousBindingException.class,
				"Found 2 potential annotation variable(s), and 2 potential argument slots");
	}

	@Test
	public void testAmbiguousAnnotationOneVar() {
		assertException(getMethod("oneAnnotation"),"@annotation(a) && @this(x)",IllegalArgumentException.class,
		"Found 2 candidate annotation binding variables but only one potential argument binding slot");
	}

	@Test
	public void testAnnotationMedley() {
		assertParameterNames(getMethod("annotationMedley"),"@annotation(a) && args(count) && this(foo)",null,"ex",
				new String[] {"ex","foo","count","a"});
	}


	public void oneAnnotation(MyAnnotation ann) {}

	public void twoAnnotations(MyAnnotation ann, MyAnnotation anotherAnn) {}

	public void annotationMedley(Throwable t, Object foo, int x, MyAnnotation ma) {}

	@interface MyAnnotation {}

}
