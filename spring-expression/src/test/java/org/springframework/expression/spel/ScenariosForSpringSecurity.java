/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.expression.spel;

import java.lang.reflect.Method;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.expression.spel.support.StandardEvaluationContext;

///CLOVER:OFF
/**
 * Spring Security scenarios from https://wiki.springsource.com/display/SECURITY/Spring+Security+Expression-based+Authorization
 *
 * @author Andy Clement
 */
public class ScenariosForSpringSecurity extends ExpressionTestCase {

	@Test
	public void testScenario01_Roles() throws Exception {
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			Expression expr = parser.parseRaw("hasAnyRole('MANAGER','TELLER')");

			ctx.setRootObject(new Person("Ben"));
			Boolean value = expr.getValue(ctx,Boolean.class);
			Assert.assertFalse(value);

			ctx.setRootObject(new Manager("Luke"));
			value = expr.getValue(ctx,Boolean.class);
			Assert.assertTrue(value);

		} catch (EvaluationException ee) {
			ee.printStackTrace();
			Assert.fail("Unexpected SpelException: " + ee.getMessage());
		}
	}

	@Test
	public void testScenario02_ComparingNames() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.addPropertyAccessor(new SecurityPrincipalAccessor());

		// Multiple options for supporting this expression: "p.name == principal.name"
		// (1) If the right person is the root context object then "name==principal.name" is good enough
		Expression expr = parser.parseRaw("name == principal.name");

		ctx.setRootObject(new Person("Andy"));
		Boolean value = expr.getValue(ctx,Boolean.class);
		Assert.assertTrue(value);

		ctx.setRootObject(new Person("Christian"));
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertFalse(value);

		// (2) Or register an accessor that can understand 'p' and return the right person
		expr = parser.parseRaw("p.name == principal.name");

		PersonAccessor pAccessor = new PersonAccessor();
		ctx.addPropertyAccessor(pAccessor);
		ctx.setRootObject(null);

		pAccessor.setPerson(new Person("Andy"));
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertTrue(value);

		pAccessor.setPerson(new Person("Christian"));
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertFalse(value);
	}

	@Test
	public void testScenario03_Arithmetic() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// Might be better with a as a variable although it would work as a property too...
		// Variable references using a '#'
		Expression expr = parser.parseRaw("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

		Boolean value = null;

		ctx.setVariable("a",1.0d); // referenced as #a in the expression
		ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertTrue(value);

		ctx.setRootObject(new Manager("Luke"));
		ctx.setVariable("a",1.043d);
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertFalse(value);
	}

	// Here i'm going to change which hasRole() executes and make it one of my own Java methods
	@Test
	public void testScenario04_ControllingWhichMethodsRun() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it);

		ctx.addMethodResolver(new MyMethodResolver()); // NEEDS TO OVERRIDE THE REFLECTION ONE - SHOW REORDERING MECHANISM
		// Might be better with a as a variable although it would work as a property too...
		// Variable references using a '#'
//		SpelExpression expr = parser.parseExpression("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");
		Expression expr = parser.parseRaw("(hasRole(3) or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

		Boolean value = null;

		ctx.setVariable("a",1.0d); // referenced as #a in the expression
		value = expr.getValue(ctx,Boolean.class);
		Assert.assertTrue(value);

//			ctx.setRootObject(new Manager("Luke"));
//			ctx.setVariable("a",1.043d);
//			value = (Boolean)expr.getValue(ctx,Boolean.class);
//			assertFalse(value);
	}


	static class Person {

		private String n;

		Person(String n) { this.n = n; }

		public String[] getRoles() { return new String[]{"NONE"}; }

		public boolean hasAnyRole(String... roles) {
			if (roles==null) return true;
			String[] myRoles = getRoles();
			for (int i=0;i<myRoles.length;i++) {
				for (int j=0;j<roles.length;j++) {
					if (myRoles[i].equals(roles[j])) return true;
				}
			}
			return false;
		}

		public boolean hasRole(String role) {
			return hasAnyRole(role);
		}

		public boolean hasIpAddress(String ipaddr) {
			return true;
		}

		public String getName() { return n; }
	}


	static class Manager extends Person {

		Manager(String n) {
			super(n);
		}

		public String[] getRoles() { return new String[]{"MANAGER"};}
	}


	static class Teller extends Person {

		Teller(String n) {
			super(n);
		}

		public String[] getRoles() { return new String[]{"TELLER"};}
	}


	static class Supervisor extends Person {

		Supervisor(String n) {
			super(n);
		}

		public String[] getRoles() { return new String[]{"SUPERVISOR"};}
	}


	static class SecurityPrincipalAccessor implements PropertyAccessor {

		static class Principal {
			public String name = "Andy";
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return name.equals("principal");
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(new Principal());
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
		}

		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}


	}


	static class PersonAccessor implements PropertyAccessor {

		Person activePerson;

		void setPerson(Person p) { this.activePerson = p; }

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return name.equals("p");
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(activePerson);
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
		}

		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

	}


	static class MyMethodResolver implements MethodResolver {

		static class HasRoleExecutor implements MethodExecutor {

			TypeConverter tc;

			public HasRoleExecutor(TypeConverter typeConverter) {
				this.tc = typeConverter;
			}

			public TypedValue execute(EvaluationContext context, Object target, Object... arguments)
					throws AccessException {
				try {
					Method m = HasRoleExecutor.class.getMethod("hasRole", String[].class);
					Object[] args = arguments;
					if (args != null) {
						ReflectionHelper.convertAllArguments(tc, args, m);
					}
					if (m.isVarArgs()) {
						args = ReflectionHelper.setupArgumentsForVarargsInvocation(m.getParameterTypes(), args);
					}
					return new TypedValue(m.invoke(null, args), new TypeDescriptor(new MethodParameter(m,-1)));
				}
				catch (Exception ex) {
					throw new AccessException("Problem invoking hasRole", ex);
				}
			}

			public static boolean hasRole(String... strings) {
				return true;
			}
		}

		public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> arguments)
				throws AccessException {
			if (name.equals("hasRole")) {
				return new HasRoleExecutor(context.getTypeConverter());
			}
			return null;
		}
	}

}
