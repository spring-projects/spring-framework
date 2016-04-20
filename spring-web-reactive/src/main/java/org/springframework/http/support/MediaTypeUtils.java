/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.support;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

/**
 * @author Arjen Poutsma
 */
public abstract class MediaTypeUtils {

	/**
	 * TODO: move to MediaType static method
	 */
	public static List<MediaType> toMediaTypes(List<MimeType> mimeTypes) {
		return mimeTypes.stream().map(MediaTypeUtils::toMediaType)
				.collect(Collectors.toList());
	}

	/**
	 * TODO: move to MediaType constructor
	 */
	public static MediaType toMediaType(MimeType mimeType) {
		return new MediaType(mimeType.getType(), mimeType.getSubtype(),
				mimeType.getParameters());
	}


}
