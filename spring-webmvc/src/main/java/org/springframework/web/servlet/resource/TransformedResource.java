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
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.springframework.core.io.ByteArrayResource;


/**
 * 
 * @author Jeremy Grelle
 */
public class TransformedResource extends ByteArrayResource {

	private final String filename;
	private final long lastModified;
	
	public TransformedResource(String filename, byte[] transformedContent) {
		super(transformedContent);
		this.filename = filename;
		this.lastModified = new Date().getTime();
	}
	
	public TransformedResource(String filename, byte[] transformedContent, long lastModified) {
		super(transformedContent);
		this.filename = filename;
		this.lastModified = lastModified;
	}

	@Override
	public String getFilename() {
		return this.filename;
	}
	
	@Override
	public long lastModified() throws IOException {
		return this.lastModified;
	}

	public String getContentAsString() {
		try {
			return new String(getByteArray(), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
}
