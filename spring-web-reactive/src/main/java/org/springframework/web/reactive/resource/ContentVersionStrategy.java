/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.resource;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

/**
 * A {@code VersionStrategy} that calculates an Hex MD5 hashes from the content
 * of the resource and appends it to the file name, e.g.
 * {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionResourceResolver
 */
public class ContentVersionStrategy extends AbstractVersionStrategy {


	public ContentVersionStrategy() {
		super(new FileNameVersionPathStrategy());
	}


	@Override
	public String getResourceVersion(Resource resource) {
		try {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			return DigestUtils.md5DigestAsHex(content);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to calculate hash for " + resource, ex);
		}
	}

}
