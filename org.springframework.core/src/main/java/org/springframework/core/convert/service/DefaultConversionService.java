/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Locale;

import org.springframework.core.convert.converter.NumberToCharacter;
import org.springframework.core.convert.converter.NumberToNumber;
import org.springframework.core.convert.converter.ObjectToString;
import org.springframework.core.convert.converter.StringToBigDecimal;
import org.springframework.core.convert.converter.StringToBigInteger;
import org.springframework.core.convert.converter.StringToBoolean;
import org.springframework.core.convert.converter.StringToByte;
import org.springframework.core.convert.converter.StringToCharacter;
import org.springframework.core.convert.converter.StringToDouble;
import org.springframework.core.convert.converter.StringToEnum;
import org.springframework.core.convert.converter.StringToFloat;
import org.springframework.core.convert.converter.StringToInteger;
import org.springframework.core.convert.converter.StringToLocale;
import org.springframework.core.convert.converter.StringToLong;
import org.springframework.core.convert.converter.StringToShort;

/**
 * Default implementation of a conversion service. Will automatically register <i>from string</i> converters for
 * a number of standard Java types like Class, Number, Boolean and so on.
 * 
 * @author Keith Donald
 */
public class DefaultConversionService extends GenericConversionService {

	/**
	 * Creates a new default conversion service, installing the default converters.
	 */
	public DefaultConversionService() {
		addDefaultConverters();
		addDefaultAliases();
	}

	/**
	 * Add all default converters to the conversion service.
	 */
	protected void addDefaultConverters() {
		addConverter(new StringToByte());
		addConverter(new StringToBoolean());
		addConverter(new StringToCharacter());
		addConverter(new StringToShort());
		addConverter(new StringToInteger());
		addConverter(new StringToLong());
		addConverter(new StringToFloat());
		addConverter(new StringToDouble());
		addConverter(new StringToBigInteger());
		addConverter(new StringToBigDecimal());
		addConverter(new StringToLocale());
		addConverter(new StringToEnum());
		addConverter(new NumberToNumber());
		addConverter(new NumberToCharacter());
		addConverter(new ObjectToString());
	}

	protected void addDefaultAliases() {
		addAlias("string", String.class);
		addAlias("byte", Byte.class);
		addAlias("boolean", Boolean.class);
		addAlias("char", Character.class);
		addAlias("short", Short.class);
		addAlias("int", Integer.class);
		addAlias("long", Long.class);
		addAlias("float", Float.class);
		addAlias("double", Double.class);
		addAlias("bigInt", BigInteger.class);
		addAlias("bigDecimal", BigDecimal.class);
		addAlias("locale", Locale.class);
		addAlias("enum", Enum.class);
		addAlias("date", Date.class);
	}

}