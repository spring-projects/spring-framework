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

package org.springframework.transaction.interceptor;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link RollbackRuleAttribute} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 * @since 09.04.2003
 */
public class RollbackRuleTests {

	@Test
	public void foundImmediatelyWithString() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Exception.class.getName());
		assertThat(rr.getDepth(new Exception())).isEqualTo(0);
	}

	@Test
	public void foundImmediatelyWithClass() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
		assertThat(rr.getDepth(new Exception())).isEqualTo(0);
	}

	@Test
	public void notFound() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.io.IOException.class.getName());
		assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(-1);
	}

	@Test
	public void ancestry() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Exception.class.getName());
		// Exception -> Runtime -> NestedRuntime -> MyRuntimeException
		assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(3);
	}

	@Test
	public void alwaysTrueForThrowable() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(java.lang.Throwable.class.getName());
		assertThat(rr.getDepth(new MyRuntimeException("")) > 0).isTrue();
		assertThat(rr.getDepth(new IOException()) > 0).isTrue();
		assertThat(rr.getDepth(new FatalBeanException(null,null)) > 0).isTrue();
		assertThat(rr.getDepth(new RuntimeException()) > 0).isTrue();
	}

	@Test
	public void ctorArgMustBeAThrowableClassWithNonThrowableType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new RollbackRuleAttribute(StringBuffer.class));
	}

	@Test
	public void ctorArgMustBeAThrowableClassWithNullThrowableType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new RollbackRuleAttribute((Class<?>) null));
	}

	@Test
	public void ctorArgExceptionStringNameVersionWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new RollbackRuleAttribute((String) null));
	}

	@Test
	public void foundEnclosedExceptionWithEnclosingException() {
		RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class);
		assertThat(rr.getDepth(new EnclosingException.EnclosedException())).isEqualTo(0);
	}

	@SuppressWarnings("serial")
	static class EnclosingException extends RuntimeException {

		@SuppressWarnings("serial")
		static class EnclosedException extends RuntimeException {

		}
	}

}
