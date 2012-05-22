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

package org.springframework.context.annotation.spr8761;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Tests cornering the regression reported in SPR-8761.
 *
 * @author Chris Beams
 */
public class Spr8761Tests {

	/**
	 * Prior to the fix for SPR-8761, this test threw because the nested MyComponent
	 * annotation was being falsely considered as a 'lite' Configuration class candidate.
	 */
	@Test
	public void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan(getClass().getPackage().getName());
		ctx.refresh();
		assertThat(ctx.containsBean("withNestedAnnotation"), is(true));
	}

}

@Component
class WithNestedAnnotation {

	@Retention(RetentionPolicy.RUNTIME)
	@Component
	public static @interface MyComponent {
	}
}
