/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link AnnotationAsyncExecutionInterceptor}.
 *
 * @author Chris Beams
 * @since 3.1.2
 */
class AnnotationAsyncExecutionInterceptorTests {

	@Test
	@SuppressWarnings("unused")
	void getExecutorQualifier() throws SecurityException, NoSuchMethodException {
		AnnotationAsyncExecutionInterceptor i = new AnnotationAsyncExecutionInterceptor(null);
		{ // method level
			class C { @Async("qMethod") void m() { } }
			assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMethod");
		}
		{ // class level
			@Async("qClass") class C { void m() { } }
			assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qClass");
		}
		{ // method and class level -> method value overrides
			@Async("qClass") class C { @Async("qMethod") void m() { } }
			assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMethod");
		}
		{ // method and class level -> method value, even if empty, overrides
			@Async("qClass") class C { @Async void m() { } }
			assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEmpty();
		}
		{ // meta annotation with qualifier
			@MyAsync class C { void m() { } }
			assertThat(i.getExecutorQualifier(C.class.getDeclaredMethod("m"))).isEqualTo("qMeta");
		}
	}

	@Async("qMeta")
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyAsync { }
}
