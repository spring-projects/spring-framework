/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

/**
 * Google Protocol Messages can contain message extensions that can be parsed if
 * the appropriate configuration has been registered in the {@code ExtensionRegistry}.
 *
 * <p>This interface provides a facility to populate the {@code ExtensionRegistry}.
 *
 * @author Alex Antonov
 * @since 4.1
 * @see <a href="https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/ExtensionRegistry">
 * com.google.protobuf.ExtensionRegistry</a>
 */
public interface ExtensionRegistryInitializer {

	/**
	 * Initializes the {@code ExtensionRegistry} with Protocol Message extensions.
	 * @param registry the registry to populate
	 */
    void initializeExtensionRegistry(ExtensionRegistry registry);

}
