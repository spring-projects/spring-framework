/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.standard;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

public class StandardTypeConverter implements TypeConverter {

	public Map<Class<?>, Map<Class<?>, StandardIndividualTypeConverter>> converters = new HashMap<Class<?>, Map<Class<?>, StandardIndividualTypeConverter>>();

	StandardTypeConverter() {
		registerConverter(new ToBooleanConverter());
		registerConverter(new ToCharacterConverter());
		registerConverter(new ToShortConverter());
		registerConverter(new ToLongConverter());
		registerConverter(new ToDoubleConverter());
		registerConverter(new ToFloatConverter());
		registerConverter(new ToStringConverter());
		registerConverter(new ToIntegerConverter());
		registerConverter(new ToByteConverter());
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		Map<Class<?>, StandardIndividualTypeConverter> possibleConvertersToTheTargetType = converters.get(targetType);
		if (possibleConvertersToTheTargetType == null && targetType.isPrimitive()) {
			if (targetType == Integer.TYPE) {
				if (sourceType == Integer.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Integer.class);
			} else if (targetType == Boolean.TYPE) {
				if (sourceType == Boolean.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Boolean.class);
			} else if (targetType == Short.TYPE) {
				if (sourceType == Short.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Short.class);
			} else if (targetType == Long.TYPE) {
				if (sourceType == Long.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Long.class);
			} else if (targetType == Character.TYPE) {
				if (sourceType == Character.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Character.class);
			} else if (targetType == Double.TYPE) {
				if (sourceType == Double.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Double.class);
			} else if (targetType == Float.TYPE) {
				if (sourceType == Float.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Float.class);
			} else if (targetType == Byte.TYPE) {
				if (sourceType == Byte.class) {
					return true;
				}
				possibleConvertersToTheTargetType = converters.get(Byte.class);
			}
		}
		if (possibleConvertersToTheTargetType != null) {
			StandardIndividualTypeConverter aConverter = possibleConvertersToTheTargetType.get(sourceType);
			if (aConverter != null) {
				return true;
			}
		}
		return false;
	}

	// TODO 3 Q In case of a loss in information with coercion to a narrower type, should we throw an exception?
	public Object convertValue(Object value, Class<?> targetType) throws SpelException {
		if (value == null || value.getClass() == targetType)
			return value;
		Class sourceType = value.getClass();
		Map<Class<?>, StandardIndividualTypeConverter> possibleConvertersToTheTargetType = converters.get(targetType);
		if (possibleConvertersToTheTargetType == null && targetType.isPrimitive()) {
			if (targetType == Integer.TYPE) {
				if (sourceType == Integer.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Integer.class);
			} else if (targetType == Boolean.TYPE) {
				if (sourceType == Boolean.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Boolean.class);
			} else if (targetType == Short.TYPE) {
				if (sourceType == Short.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Short.class);
			} else if (targetType == Long.TYPE) {
				if (sourceType == Long.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Long.class);
			} else if (targetType == Character.TYPE) {
				if (sourceType == Character.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Character.class);
			} else if (targetType == Double.TYPE) {
				if (sourceType == Double.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Double.class);
			} else if (targetType == Float.TYPE) {
				if (sourceType == Float.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Float.class);
			} else if (targetType == Byte.TYPE) {
				if (sourceType == Byte.class) {
					return value;
				}
				possibleConvertersToTheTargetType = converters.get(Byte.class);
			}
		}
		Object result = null;
		if (possibleConvertersToTheTargetType != null) {
			StandardIndividualTypeConverter aConverter = possibleConvertersToTheTargetType.get(value.getClass());
			if (aConverter != null) {
				try {
					result = aConverter.convert(value);
				} catch (EvaluationException ee) {
					if (ee instanceof SpelException) {
						throw (SpelException) ee;
					} else {
						throw new SpelException(SpelMessages.PROBLEM_DURING_TYPE_CONVERSION, ee.getMessage());
					}
				}
			}
		}
		if (result != null)
			return result;
		throw new SpelException(SpelMessages.TYPE_CONVERSION_ERROR, value.getClass(), targetType);
	}

	public void registerConverter(StandardIndividualTypeConverter aConverter) {
		Class<?> toType = aConverter.getTo();
		Map<Class<?>, StandardIndividualTypeConverter> convertersResultingInSameType = converters.get(toType);
		if (convertersResultingInSameType == null) {
			convertersResultingInSameType = new HashMap<Class<?>, StandardIndividualTypeConverter>();
		}
		Class<?>[] fromTypes = aConverter.getFrom();
		for (int i = 0; i < fromTypes.length; i++) {
			convertersResultingInSameType.put(fromTypes[i], aConverter);
		}
		converters.put(aConverter.getTo(), convertersResultingInSameType);
	}

	private static class ToBooleanConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			if (value instanceof Integer) {
				return ((Integer) value).intValue() != 0;
			} else {
				return ((Long) value).longValue() != 0;
			}
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class, Long.class };
		}

		public Class<?> getTo() {
			return Boolean.class;
		}
	}

	private static class ToDoubleConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			if (value instanceof Double) {
				return ((Double) value).doubleValue();
			} else if (value instanceof String) {
				try {
					Double.parseDouble((String) value);
				} catch (NumberFormatException nfe) {
					// TODO 3 Q throw something or leave the caller to throw a conversion exception?
				}
			} else if (value instanceof Integer) {
				return new Double(((Integer) value).intValue());
			}
			return null;
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Double.class, String.class, Integer.class };
		}

		public Class<?> getTo() {
			return Double.class;
		}
	}

	private static class ToFloatConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			return ((Double) value).floatValue();
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Double.class };
		}

		public Class<?> getTo() {
			return Float.class;
		}
	}

	private static class ToByteConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			return ((Integer) value).byteValue();
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class };
		}

		public Class<?> getTo() {
			return Byte.class;
		}
	}

	private static class ToLongConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			if (value instanceof Integer)
				return ((Integer) value).longValue();
			else if (value instanceof Short)
				return ((Short) value).longValue();
			else if (value instanceof Byte)
				return ((Byte) value).longValue();
			return null;
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class, Short.class, Byte.class };
		}

		public Class<?> getTo() {
			return Long.class;
		}
	}

	private static class ToCharacterConverter implements StandardIndividualTypeConverter {
		public Character convert(Object value) throws SpelException {
			if (value instanceof Integer)
				return ((char) ((Integer) value).intValue());
			if (value instanceof String) {
				String s = (String) value;
				if (s.length() == 1)
					return s.charAt(0);
			}
			return null;
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class, String.class };
		}

		public Class<?> getTo() {
			return Character.class;
		}
	}

	private static class ToShortConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			if (value instanceof Integer)
				return ((short) ((Integer) value).shortValue());
			return null;
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class };
		}

		public Class<?> getTo() {
			return Short.class;
		}
	}

	private static class ToStringConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			return value.toString();
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Integer.class, Double.class };
		}

		public Class<?> getTo() {
			return String.class;
		}
	}

	private static class ToIntegerConverter implements StandardIndividualTypeConverter {
		public Object convert(Object value) throws SpelException {
			try {
				return Integer.parseInt(((Long) value).toString());
			} catch (NumberFormatException nfe) {
				throw new SpelException(SpelMessages.PROBLEM_DURING_TYPE_CONVERSION, "long value '" + value
						+ "' cannot be represented as an int");
			}
		}

		public Class<?>[] getFrom() {
			return new Class<?>[] { Long.class };
		}

		public Class<?> getTo() {
			return Integer.class;
		}
	}

}
