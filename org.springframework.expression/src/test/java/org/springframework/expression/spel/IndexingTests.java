package org.springframework.expression.spel;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class IndexingTests {

	@Test
	@Ignore
	public void emptyList() {
		listOfScalarNotGeneric = new ArrayList();
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfScalarNotGeneric");
		assertEquals("java.util.List<java.lang.Object>", expression.getValueTypeDescriptor(this).toString());
		assertEquals("", expression.getValue(this, String.class));
	}

	@Test
	@Ignore
	public void resolveCollectionElementType() {
		listNotGeneric = new ArrayList();
		listNotGeneric.add(5);
		listNotGeneric.add(6);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listNotGeneric");
		assertEquals("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.List<@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.lang.Integer>", expression.getValueTypeDescriptor(this).toString());
		assertEquals("5,6", expression.getValue(this, String.class));
	}

	@Test
	public void resolveCollectionElementTypeNull() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listNotGeneric");
		assertEquals("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.List<@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.lang.Object>", expression.getValueTypeDescriptor(this).toString());
	}

	@FieldAnnotation
	public List listNotGeneric;

	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldAnnotation {
		
	}

	@Test
	public void resolveMapKeyValueTypes() {
		mapNotGeneric = new HashMap();
		mapNotGeneric.put("baseAmount", 3.11);
		mapNotGeneric.put("bonusAmount", 7.17);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("mapNotGeneric");
		assertEquals("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.Map<java.lang.String, java.lang.Double>", expression.getValueTypeDescriptor(this).toString());
	}
	
	@FieldAnnotation
	public Map mapNotGeneric;

	@Test
	public void testListOfScalar() {
		listOfScalarNotGeneric = new ArrayList();
		listOfScalarNotGeneric.add("5");
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfScalarNotGeneric[0]");
		assertEquals(new Integer(5), expression.getValue(this, Integer.class));
	}
	
	public List listOfScalarNotGeneric;

	
	@Test
	public void testListsOfMap() {
		listOfMapsNotGeneric = new ArrayList();
		Map map = new HashMap();
		map.put("fruit", "apple");
		listOfMapsNotGeneric.add(map);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfMapsNotGeneric[0]['fruit']");
		assertEquals("apple", expression.getValue(this, String.class));
	}
	
	public List listOfMapsNotGeneric;
	
}
