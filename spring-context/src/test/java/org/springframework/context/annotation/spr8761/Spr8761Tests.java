/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr8761;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests cornering the regression reported in SPR-8761.
 *
 * @author Chris Beams
 */
class Spr8761Tests {

	/**
	 * Prior to the fix for SPR-8761, this test threw because the nested MyComponent
	 * annotation was being falsely considered as a 'lite' Configuration class candidate.
	 */
	@Test
	void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan(getClass().getPackage().getName());
		ctx.refresh();
		assertThat(ctx.containsBean("withNestedAnnotation")).isTrue();
		ctx.close();
	}

}

@Component
class WithNestedAnnotation {

	@Retention(RetentionPolicy.RUNTIME)
	@Component
	@interface MyComponent {
	}
}
