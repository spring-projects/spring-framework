/*
 * Copyright 2002-2023 the original author or authors.
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

package test.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect("perthis(test.aspect.CommonPointcuts.getAgeExecution())")
public class PerThisAspect {

	private int invocations = 0;

	public int getInvocations() {
		return this.invocations;
	}

	@Around("test.aspect.CommonPointcuts.getAgeExecution()")
	public int changeAge(ProceedingJoinPoint pjp) {
		return this.invocations++;
	}

}
