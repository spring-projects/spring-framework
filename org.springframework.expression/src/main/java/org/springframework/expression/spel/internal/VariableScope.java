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

import java.util.HashMap;
import java.util.Map;

/**
 * A new scope is entered when a function is called and it is used to hold the parameters to the function call.  If the names
 * of the parameters clash with those in a higher level scope, those in the higher level scope will not be accessible whilst
 * the function is executing.  When the function returns the scope is exited.
 * 
 * @author Andy Clement
 *
 */
public class VariableScope {

	private final Map<String, Object> vars = new HashMap<String, Object>();

	public VariableScope() { }

	public VariableScope(Map<String, Object> arguments) {
		if (arguments!=null) {
			vars.putAll(arguments);
		}
	}
	
	public VariableScope(String name,Object value) {
		vars.put(name,value);
	}

	public Object lookupVariable(String name) {
		return vars.get(name);
	}

	public void setVariable(String name, Object value) {
		vars.put(name,value);
	}

	public boolean definesVariable(String name) {
		return vars.containsKey(name);
	}

}