/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression;

/**
 * If a property accessor is built upon the CacheablePropertyAccessor class then once the property
 * has been resolved the accessor will return an instance of this PropertyWriterExecutor interface
 * that can be cached and repeatedly called to set the value of the property. 
 * <p>
 * They can become stale, and in that case should throw an AccessException - this will cause the 
 * infrastructure to go back to the resolvers to ask for a new one.
 *
 * @author Andy Clement
 */
public interface PropertyWriterExecutor {

	/**
	 * Set the value of a property to the supplied new value.
	 *
	 * @param context the evaluation context in which the command is being executed
	 * @param targetObject the target object on which property write is being attempted
	 * @param newValue the new value for the property
	 * @throws AccessException if there is a problem setting the property or this executor has become stale
	 */
	void execute(EvaluationContext evaluationContext, Object targetObject, Object newValue) throws AccessException;

}
