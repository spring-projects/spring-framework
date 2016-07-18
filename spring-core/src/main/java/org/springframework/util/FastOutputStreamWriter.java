/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * The Class FastOutputStreamWriter.
 * test in tomcat 9 , thread pool , http response with a  15k json  ,the below writer can improve the tps 900 to more than 1000 and save memory;
 *
 * @author niaoge
 * @since 4.3
 */
public class FastOutputStreamWriter extends OutputStreamWriter {

	private static final int DEFAULT_CHAR_LENGTH = 4096;
	private char[] charbuf =null; 


	public FastOutputStreamWriter(OutputStream out) {
		super(out);
	}

	public FastOutputStreamWriter(OutputStream out, String charsetName)
			throws UnsupportedEncodingException {
		super(out, charsetName);
	}

	public FastOutputStreamWriter(OutputStream out, Charset cs) {
		super(out, cs);
	}

	/**
	 * if the param str is a bigger str, this method will save allmost half of memory  
	 */
	@Override
	public void write(String str, int off, int len) throws IOException {
		charbuf =new char[DEFAULT_CHAR_LENGTH];
		int times =len / DEFAULT_CHAR_LENGTH;
		int left =len % DEFAULT_CHAR_LENGTH ;
		
		for (int i = 0; i < times; i++) {
			str.getChars(off, off+DEFAULT_CHAR_LENGTH, charbuf, 0);
			this.write(charbuf,0,DEFAULT_CHAR_LENGTH);
			off+=DEFAULT_CHAR_LENGTH;
		}
		
		if (left>0){
			str.getChars(off, off+left, charbuf, 0);
			this.write(charbuf, 0, left);
		}
		charbuf =null;
	}

}
