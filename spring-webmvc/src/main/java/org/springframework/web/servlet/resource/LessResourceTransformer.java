/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;


/**
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
public class LessResourceTransformer implements ResourceTransformer {

	private static final String LESS_EXT = "less";

	private final LessCompiler compiler = new LessCompiler();


	@Override
	public Resource transform(Resource original) throws IOException {
		TransformedResource transformed;
		try {
			String content = "";
			if (original instanceof TransformedResource) {
				content = ((TransformedResource) original).getContentAsString();
			}
			else {
				content = this.compiler.compile(original.getFile());
			}
			transformed = new TransformedResource(original.getFilename().replace(
					"." + LESS_EXT, ""), content.getBytes("UTF-8"), original.lastModified());
		}
		catch (LessException ex) {
			//TODO - Nicely print out the compilation error
			ex.printStackTrace();
			return null;
		}
		return transformed;
	}

	@Override
	public boolean handles(HttpServletRequest request, Resource original) {
		return LESS_EXT.equals(StringUtils.getFilenameExtension(original.getFilename()));
	}

}
