/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.validation.method;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;

/**
 * {@link MethodValidationResult} with an empty list of results.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
final class EmptyMethodValidationResult implements MethodValidationResult {

	@Override
	public Object getTarget() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Method getMethod() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isForReturnValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ParameterValidationResult> getParameterValidationResults() {
		return Collections.emptyList();
	}

	@Override
	public List<MessageSourceResolvable> getCrossParameterValidationResults() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return "0 validation errors";
	}

}
