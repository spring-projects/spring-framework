/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert;

/**
 * A command object that is parameterized with the information necessary to perform a conversion of a source input to a
 * target output. Encapsulates knowledge about how to convert source objects to a specific target type using a specific
 * converter.
 * 
 * @author Keith Donald
 */
public interface ConversionExecutor<S, T> {

	/**
	 * Returns the source class of conversions performed by this executor.
	 * @return the source class
	 */
	public Class<S> getSourceClass();

	/**
	 * Returns the target class of conversions performed by this executor.
	 * @return the target class
	 */
	public Class<T> getTargetClass();

	/**
	 * Execute the conversion for the provided source object.
	 * @param source the source object to convert
	 */
	public T execute(S source) throws ConversionExecutionException;

}