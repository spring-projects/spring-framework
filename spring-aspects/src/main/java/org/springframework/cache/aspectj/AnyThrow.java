/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.aspectj;

/**
 * Utility to trick the compiler to throw a valid checked
 * exceptions within the interceptor.
 *
 * @author Stephane Nicoll
 */
class AnyThrow {

	static void throwUnchecked(Throwable e) {
		AnyThrow.<RuntimeException>throwAny(e);
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwAny(Throwable e) throws E {
		throw (E) e;
	}
}
