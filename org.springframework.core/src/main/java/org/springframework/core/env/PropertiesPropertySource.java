/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.env;

import java.util.Properties;


/**
 * TODO SPR-7508: document how this does accept a Properties object,
 * which is capable of holding non-string keys and values (because
 * Properties is a Hashtable),  but is limited to resolving string-based
 * keys and values.
 *
 * Consider adding a TypeConvertingPropertiesPropertySource to accommodate
 * non-string keys and values (such as is technically possible with
 * System.getProperties())
 *
 * @author Chris Beams
 * @since 3.1
 */
public class PropertiesPropertySource extends PropertySource<Properties> {

	public PropertiesPropertySource(String name, Properties source) {
		super(name, source);
	}

	public boolean containsProperty(String key) {
		return source.containsKey(key);
	}

	public String getProperty(String key) {
		return source.getProperty(key);
	}

	@Override
	public int size() {
		return source.size();
	}

}
