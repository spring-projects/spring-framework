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

package org.springframework.resilience;

import java.lang.reflect.Method;
import java.nio.charset.MalformedInputException;

import org.springframework.resilience.retry.MethodRetryPredicate;

class RejectMalformedInputException3Predicate implements MethodRetryPredicate {

	@Override
	public boolean shouldRetry(Method method, Throwable throwable) {
		return !(throwable.getClass() == MalformedInputException.class && throwable.getMessage().contains("3"));
	}

}
