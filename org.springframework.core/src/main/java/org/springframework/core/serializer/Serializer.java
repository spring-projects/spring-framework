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

package org.springframework.core.serializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A strategy interface for streaming an object to an OutputStream.
 * 
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public interface Serializer<T> {

	/**
	 * Write an object of type T to the outputSream.
	 * @param object The object.
	 * @param outputStream the outputStream
	 * @throws IOException in case of errors writing to the stream
	 */
	public void serialize(T object, OutputStream outputStream) throws IOException;

}
