/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpEL expression tests for Spring Security scenarios.
 *
 * @author Andy Clement
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#authorization-expressions">Expressing Authorization with SpEL</a>
 */
class ScenariosForSpringSecurityExpressionTests extends AbstractExpressionTests {

	@Test
	void testScenario01_Roles() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		Expression expr = parser.parseRaw("hasAnyRole('MANAGER','TELLER')");

		ctx.setRootObject(new Person("Ben"));
		Boolean value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isFalse();

		ctx.setRootObject(new Manager("Luke"));
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isTrue();
	}

	@Test
	void testScenario02_ComparingNames() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.addPropertyAccessor(new SecurityPrincipalAccessor());

		// Multiple options for supporting this expression: "p.name == principal.name"
		// (1) If the right person is the root context object then "name==principal.name" is good enough
		Expression expr = parser.parseRaw("name == principal.name");

		ctx.setRootObject(new Person("Andy"));
		Boolean value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isTrue();

		ctx.setRootObject(new Person("Christian"));
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isFalse();

		// (2) Or register an accessor that can understand 'p' and return the right person
		expr = parser.parseRaw("p.name == principal.name");

		PersonAccessor pAccessor = new PersonAccessor();
		ctx.addPropertyAccessor(pAccessor);
		ctx.setRootObject(null);

		pAccessor.setPerson(new Person("Andy"));
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isTrue();

		pAccessor.setPerson(new Person("Christian"));
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isFalse();
	}

	@Test
	void testScenario03_Arithmetic() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// Might be better with a as a variable although it would work as a property too...
		// Variable references using a '#'
		Expression expr = parser.parseRaw("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

		Boolean value = null;

		ctx.setVariable("a",1.0d); // referenced as #a in the expression
		ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isTrue();

		ctx.setRootObject(new Manager("Luke"));
		ctx.setVariable("a",1.043d);
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isFalse();
	}

	// Here i'm going to change which hasRole() executes and make it one of my own Java methods
	@Test
	void testScenario04_ControllingWhichMethodsRun() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it;

		ctx.addMethodResolver(new MyMethodResolver()); // NEEDS TO OVERRIDE THE REFLECTION ONE - SHOW REORDERING MECHANISM
		// Might be better with a as a variable although it would work as a property too...
		// Variable references using a '#'
//		SpelExpression expr = parser.parseExpression("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");
		Expression expr = parser.parseRaw("(hasRole(3) or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

		Boolean value = null;

		ctx.setVariable("a",1.0d); // referenced as #a in the expression
		value = expr.getValue(ctx,Boolean.class);
		assertThat((boolean) value).isTrue();

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
			if (roles == null) {
				return true;
			}
			String[] myRoles = getRoles();
			for (String myRole : myRoles) {
				for (String role : roles) {
					if (myRole.equals(role)) {
						return true;
					}
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

		@Override
		public String[] getRoles() { return new String[]{"MANAGER"};}
	}


	static class Teller extends Person {

		Teller(String n) {
			super(n);
		}

		@Override
		public String[] getRoles() { return new String[]{"TELLER"};}
	}


	static class Supervisor extends Person {

		Supervisor(String n) {
			super(n);
		}

		@Override
		public String[] getRoles() { return new String[]{"SUPERVISOR"};}
	}


	static class SecurityPrincipalAccessor implements PropertyAccessor {

		static class Principal {
			public String name = "Andy";
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return name.equals("principal");
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(new Principal());
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}


	}


	static class PersonAccessor implements PropertyAccessor {

		Person activePerson;

		void setPerson(Person p) { this.activePerson = p; }

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return name.equals("p");
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(activePerson);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}

		@Override
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

			@Override
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

		@Override
		public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> arguments) {
			if (name.equals("hasRole")) {
				return new HasRoleExecutor(context.getTypeConverter());
			}
			return null;
		}
	}

}
