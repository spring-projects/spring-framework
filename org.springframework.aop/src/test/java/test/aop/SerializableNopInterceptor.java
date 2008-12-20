/*
 * Copyright 2002-2008 the original author or authors.
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

package test.aop;

import java.io.Serializable;



/**
 * Subclass of NopInterceptor that is serializable and
 * can be used to test proxy serialization.
 * 
 * @author Rod Johnson
 * @author Chris Beams
 */
@SuppressWarnings("serial")
public class SerializableNopInterceptor extends NopInterceptor implements Serializable {
	
	/**
	 * We must override this field and the related methods as
	 * otherwise count won't be serialized from the non-serializable
	 * NopInterceptor superclass.
	 */
	private int count;
	
	public int getCount() {
		return this.count;
	}
	
	protected void increment() {
		++count;
	}
	
}