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

package org.springframework.http.converter.multipart;

import org.springframework.util.LinkedMultiValueMap;

/**
 * Represents HTTP multipart form data, mapping names to parts.
 *
 * <p>In addition to the normal methods defined by {@link org.springframework.util.MultiValueMap}, this class offers
 * the following convenience methods:
 * <ul>
 * <li>{@link #addTextPart} to add a text part (i.e. a form field)</li>
 * <li>{@link #addBinaryPart} to add a binary part (i.e. a file)</li>
 * <li>{@link #addPart} to add a custom part</li>
 * </ul>
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 */
public class MultipartMap extends LinkedMultiValueMap<String, Object> {

}
