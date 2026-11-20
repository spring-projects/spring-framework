/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

/**
 * A {@code VersionStrategy} that calculates a Hex MD5 hash from the content
 * of the resource and appends it to the file name, for example,
 * {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
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
