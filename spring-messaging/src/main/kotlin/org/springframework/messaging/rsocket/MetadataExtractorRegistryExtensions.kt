/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.rsocket

import org.springframework.util.MimeType

/**
 * Extension for [MetadataExtractorRegistry.metadataToExtract] providing a `metadataToExtract<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param mimeType the mime type of metadata entries to extract
 * @param name assign a name for the decoded value; if not provided, then
 * the mime type is used as the key
 * @param T the target value type to decode to
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any > MetadataExtractorRegistry.metadataToExtract(mimeType: MimeType, name: String? = null) =
		metadataToExtract(mimeType, object : org.springframework.core.ParameterizedTypeReference<T>() {}, name)

/**
 * Extension for [MetadataExtractorRegistry.metadataToExtract] providing a `metadataToExtract<Foo>(...)`
 * variant leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param mimeType the mime type of metadata entries to extract
 * @param mapper custom logic to add the decoded value to the output map
 * @param T the target value type to decode to
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any > MetadataExtractorRegistry.metadataToExtract(mimeType: MimeType, noinline mapper: (T, MutableMap<String, Any>) -> Unit) =
		metadataToExtract(mimeType, object : org.springframework.core.ParameterizedTypeReference<T>() {}, mapper)

