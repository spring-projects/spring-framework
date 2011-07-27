/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.bind.support;

import org.springframework.web.bind.annotation.FlashAttributes;


/**
 * Simple interface to pass into controller methods to allow them to activate 
 * a mode in which model attributes identified as "flash attributes" are 
 * temporarily stored in the session to make them available to the next 
 * request. The most common scenario is a client-side redirect.
 * 
 * <p>In active mode, model attributes that match the attribute names or 
 * types declared via @{@link FlashAttributes} are saved in the session. 
 * On the next request, any flash attributes found in the session are 
 * automatically added to the model of the target controller method and 
 * are also cleared from the session. 
 * 
 * TODO ...
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface FlashStatus {

	/**
	 * TODO ...
	 */
	void setActive();
	
	/**
	 * TODO ...
	 */
	boolean isActive();
	
}
