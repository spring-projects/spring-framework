/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.File;

import reactor.core.publisher.Mono;

/**
 * Specialization of {@link Part} for a file upload.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface FilePart extends Part {

	/**
	 * Return the original filename in the client's filesystem.
	 */
	String filename();

	/**
	 * Transfer the file in this part to the given file destination.
	 * @param dest the target file
	 * @return completion {@code Mono} with the result of the file transfer,
	 * possibly {@link IllegalStateException} if the part isn't a file
	 */
	Mono<Void> transferTo(File dest);

}
