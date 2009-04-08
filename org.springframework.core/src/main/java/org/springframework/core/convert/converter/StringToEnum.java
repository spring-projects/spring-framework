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
package org.springframework.core.convert.converter;

/**
 * Converts a String to a Enum using {@link Enum#valueOf(Class, String)}.
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class StringToEnum implements SuperTwoWayConverter<String, Enum> {

	public <RT extends Enum> RT convert(String source, Class<RT> targetClass) {
		return (RT) Enum.valueOf(targetClass, source);
	}

	public <RS extends String> RS convertBack(Enum target, Class<RS> sourceClass) {
		return (RS) target.name();
	}

}
