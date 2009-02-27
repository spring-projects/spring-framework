/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.util;

import org.springframework.beans.factory.config.BeanDefinition;


/**
 * Constant class contains the names of the scopes supported out of the box in Spring 2.0.
 *
 * @author  Costin Leau
 * @author  Chris Beams
 */
public abstract class DefaultScopes {

    public static final String SINGLETON = BeanDefinition.SCOPE_SINGLETON;

    public static final String PROTOTYPE = BeanDefinition.SCOPE_PROTOTYPE;

    public static final String REQUEST = "request"; // see WebApplicationContext.SCOPE_REQUEST;

    public static final String SESSION = "session"; // see WebApplicationContext.SCOPE_SESSION;

}
