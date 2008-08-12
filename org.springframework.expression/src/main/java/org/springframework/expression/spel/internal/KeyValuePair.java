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
package org.springframework.expression.spel.internal;

/**
 * Special object that is used to wrap a map entry/value when iterating over a map.  Providing a direct way for the 
 * expression to refer to either the key or value.
 * 
 * @author Andy Clement
 */
public class KeyValuePair {
	public Object key;
	public Object value;
	
	public KeyValuePair(Object k, Object v) {
		this.key = k;
		this.value = v;
	}

}
