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
package org.springframework.core.convert.service;

import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;

/**
 * Conversion executor that does nothing.  Access singleton at {@link #INSTANCE}.s
 */
class NoOpConversionExecutor implements ConversionExecutor {

	public static final ConversionExecutor INSTANCE = new NoOpConversionExecutor();
	
	private NoOpConversionExecutor() {
		
	}
	
	public Object execute(Object source) throws ConversionExecutionException {
		return source;
	}

}