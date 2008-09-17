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
package org.springframework.expression.spel;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.springframework.expression.common.CompositeStringExpressionTests;
import org.springframework.expression.common.LiteralExpressionTests;

/**
 * Pulls together all the tests for Spring EL into a single suite.
 * 
 * @author Andy Clement
 * 
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Spring Expression Language tests");
		// $JUnit-BEGIN$
		suite.addTestSuite(BooleanExpressionTests.class);
		suite.addTestSuite(LiteralTests.class);
		suite.addTestSuite(ParsingTests.class);
		suite.addTestSuite(VariableAndFunctionTests.class);
		suite.addTestSuite(ParserErrorMessagesTests.class);
		suite.addTestSuite(EvaluationTests.class);
		suite.addTestSuite(OperatorTests.class);
		suite.addTestSuite(ConstructorInvocationTests.class);
		suite.addTestSuite(MethodInvocationTests.class);
		suite.addTestSuite(PropertyAccessTests.class);
		suite.addTestSuite(TypeReferencing.class);
		suite.addTestSuite(PerformanceTests.class);
		suite.addTestSuite(DefaultComparatorUnitTests.class);
		suite.addTestSuite(TemplateExpressionParsingTests.class);
		suite.addTestSuite(ExpressionLanguageScenarioTests.class);
		suite.addTestSuite(ScenariosForSpringSecurity.class);
		suite.addTestSuite(MapAccessTests.class);
		suite.addTestSuite(SpelUtilitiesTests.class);
		suite.addTestSuite(LiteralExpressionTests.class);
		suite.addTestSuite(CompositeStringExpressionTests.class);
		// $JUnit-END$
		return suite;
	}

}
