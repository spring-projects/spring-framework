/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.context.annotation;


/**
 * Enumerates the names of the scopes supported out of the box in Spring.
 * 
 * <p>Not modeled as an actual java enum because annotations that accept a scope attribute
 * must allow for user-defined scope names. Given that java enums are not extensible, these
 * must remain simple string constants.
 * 
 * @author Chris Beams
 * @since 3.0
 */
public class StandardScopes {

	private StandardScopes() { }

	public static final String SINGLETON = "singleton";

	public static final String PROTOTYPE = "prototype";

	public static final String REQUEST = "request";

	public static final String SESSION = "session";

}

// TODO: move StandardScopes to appropriate package
