/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj.autoproxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.ProceedingJoinPoint;


/**
 * Definitions of testing types for use in within this package.
 * Wherever possible, test types should be defined local to the java
 * file that makes use of them.  In some cases however, a test type may
 * need to be shared across tests.  Such types reside here, with the
 * intention of reducing the surface area of java files within this
 * package.  This allows developers to think about tests first, and deal
 * with these second class testing artifacts on an as-needed basis.
 *
 * Types here should be defined as package-private top level classes in
 * order to avoid needing to fully qualify, e.g.: _TestTypes$Foo.
 *
 * @author Chris Beams
 */
final class _TestTypes { }


/**
 * @author Adrian Colyer
 * @since 2.0
 */
interface AnnotatedTestBean {

	String doThis();

	String doThat();

	String doTheOther();

	String[] doArray();

}


/**
 * @author Adrian Colyer
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@interface TestAnnotation {
	String value() ;
}


/**
 * @author Adrian Colyer
 * @since 2.0
 */
class AnnotatedTestBeanImpl implements AnnotatedTestBean {

	@Override
	@TestAnnotation("this value")
	public String doThis() {
		return "doThis";
	}

	@Override
	@TestAnnotation("that value")
	public String doThat() {
		return "doThat";
	}

	@Override
	@TestAnnotation("array value")
	public String[] doArray() {
		return new String[] {"doThis", "doThat"};
	}

	// not annotated
	@Override
	public String doTheOther() {
		return "doTheOther";
	}

}


/**
 * @author Adrian Colyer
 */
class AnnotationBindingTestAspect {

	public String doWithAnnotation(ProceedingJoinPoint pjp, TestAnnotation testAnnotation) throws Throwable {
		return testAnnotation.value();
	}

}
