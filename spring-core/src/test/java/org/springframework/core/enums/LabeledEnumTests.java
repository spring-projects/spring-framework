/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.enums;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class LabeledEnumTests extends TestCase {

	private byte[] serializeObject(final Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.close();
		return baos.toByteArray();
	}

	private Object deserializeObject(final byte[] serializedBytes) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedBytes));
		Object obj = ois.readObject();
		ois.close();
		return obj;
	}

	private Object serializeAndDeserializeObject(Object obj) throws IOException, ClassNotFoundException {
		return deserializeObject(serializeObject(obj));
	}

	public void testCodeFound() {
		Dog golden = (Dog) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(Dog.class, new Short((short) 11));
		Dog borderCollie = (Dog) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(Dog.class,
				new Short((short) 13));
		assertSame(golden, Dog.GOLDEN_RETRIEVER);
		assertSame(borderCollie, Dog.BORDER_COLLIE);
	}

	public void testCodeFoundForAbstractEnums() {
		ValuedEnum one = (ValuedEnum) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(ValuedEnum.class,
				new Short((short) 1));
		ValuedEnum two = (ValuedEnum) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(ValuedEnum.class,
				new Short((short) 2));
		assertSame(one, ValuedEnum.ONE);
		assertSame(two, ValuedEnum.TWO);
	}

	public void testDeserializationOfInnerClassEnums() throws Exception {
		assertSame(serializeAndDeserializeObject(Other.THING1), Other.THING1);
	}

	public void testDeserializationOfStandAloneEnums() throws Exception {
		assertSame(serializeAndDeserializeObject(StandAloneStaticLabeledEnum.ENUM1),
				StandAloneStaticLabeledEnum.ENUM1);
	}

	public void testLabelFound() {
		Dog golden = (Dog) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(Dog.class, "Golden Retriever");
		Dog borderCollie = (Dog) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(Dog.class, "Border Collie");
		assertSame(golden, Dog.GOLDEN_RETRIEVER);
		assertSame(borderCollie, Dog.BORDER_COLLIE);
	}

	public void testLabelFoundForStandAloneEnum() {
		StandAloneStaticLabeledEnum enum1 = (StandAloneStaticLabeledEnum)
				StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(StandAloneStaticLabeledEnum.class, "Enum1");
		StandAloneStaticLabeledEnum enum2 = (StandAloneStaticLabeledEnum)
				StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(StandAloneStaticLabeledEnum.class, "Enum2");
		assertSame(enum1, StandAloneStaticLabeledEnum.ENUM1);
		assertSame(enum2, StandAloneStaticLabeledEnum.ENUM2);
	}

	public void testLabelFoundForAbstractEnums() {
		ValuedEnum one = (ValuedEnum)
				StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(ValuedEnum.class, "one");
		ValuedEnum two = (ValuedEnum)
				StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(ValuedEnum.class, "two");
		assertSame(one, ValuedEnum.ONE);
		assertSame(two, ValuedEnum.TWO);
	}

	public void testDoesNotMatchWrongClass() {
		try {
			LabeledEnum none = StaticLabeledEnumResolver.instance().getLabeledEnumByCode(Dog.class,
					new Short((short) 1));
			fail("Should have failed");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testEquals() {
		assertEquals("Code equality means equals", Dog.GOLDEN_RETRIEVER, new Dog(11, "Golden Retriever"));
		assertFalse("Code inequality means notEquals", Dog.GOLDEN_RETRIEVER.equals(new Dog(12, "Golden Retriever")));
	}


	@SuppressWarnings("serial")
	private static class Other extends StaticLabeledEnum {

		public static final Other THING1 = new Other(1, "Thing1");
		public static final Other THING2 = new Other(2, "Thing2");


		private Other(int code, String name) {
			super(code, name);
		}
	}


	@SuppressWarnings("serial")
	private static class Dog extends StaticLabeledEnum {

		public static final Dog GOLDEN_RETRIEVER = new Dog(11, null) {

			@Override
			public String getLabel() {
				return "Golden Retriever";
			}

			// Overriding getType() is no longer necessary as of Spring 2.5;
			// however, this is left here to provide valid testing for
			// backwards compatibility.
			@Override
			public Class getType() {
				return Dog.class;
			}
		};

		public static final Dog BORDER_COLLIE = new Dog(13, "Border Collie");
		public static final Dog WHIPPET = new Dog(14, "Whippet");

		// Ignore this
		public static final Other THING1 = Other.THING1;


		private Dog(int code, String name) {
			super(code, name);
		}
	}


	@SuppressWarnings("serial")
	private static abstract class ValuedEnum extends StaticLabeledEnum {

		public static final ValuedEnum ONE = new ValuedEnum(1, "one") {
			@Override
			public int getValue() {
				return 1;
			}
		};

		public static final ValuedEnum TWO = new ValuedEnum(2, "two") {
			@Override
			public int getValue() {
				return 2;
			}
		};

		private ValuedEnum(int code, String name) {
			super(code, name);
		}

		public abstract int getValue();
	}

}
