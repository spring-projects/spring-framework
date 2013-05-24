/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.aop.framework;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ObjenesisSupport {
	private final static Log logger = LogFactory.getLog(ObjenesisSupport.class);
	
    public static boolean isObjenesisAvaliable = checkIsObjenesisAvaliable();
    
    private static boolean checkIsObjenesisAvaliable(){
    	try{
    		Class.forName("org.objenesis.Objenesis");
    		return true;
    	}catch(ClassNotFoundException e){
    		logger.warn("Objenesis is not available, proxies will be created using constructor - possible side-effects");
    		return false;
    	}
    }
    
    public static boolean isObjenesisAvaliable(){
    	return isObjenesisAvaliable;
    }
    
    public Object newInstance(Class clazz){
    	if (!isObjenesisAvaliable){
    		throw new IllegalStateException("no org.objenesis.Objenesis found in classpath");
    	}
    	
    	return new ObjenesisStd().newInstance(clazz);
    }
}
