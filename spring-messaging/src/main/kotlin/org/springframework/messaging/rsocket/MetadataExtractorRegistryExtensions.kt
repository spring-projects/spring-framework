/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

