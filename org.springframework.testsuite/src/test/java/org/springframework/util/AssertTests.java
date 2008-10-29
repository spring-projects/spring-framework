/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.util.*;

/**
 * Unit tests for the {@link Assert} class.
 *
 * @author Keith Donald
 * @author Erwin Vervaet
 * @author Rick Evans
 */
public class AssertTests extends TestCase {

    public void testInstanceOf() {
        final Set set = new HashSet();
        Assert.isInstanceOf(HashSet.class, set);
        new AssertThrows(IllegalArgumentException.class, "a hash map is not a set") {
            public void test() throws Exception {
                Assert.isInstanceOf(HashMap.class, set);
            }
        }.runTest();
    }

    public void testIsNullDoesNotThrowExceptionIfArgumentIsNullWithMessage() {
        Assert.isNull(null, "Bla");
    }

    public void testIsNullDoesNotThrowExceptionIfArgumentIsNull() {
        Assert.isNull(null);
    }

    public void testIsNullThrowsExceptionIfArgumentIsNotNull() {
        new AssertThrows(IllegalArgumentException.class, "object is not null") {
            public void test() throws Exception {
                Assert.isNull(new Object());
            }
        }.runTest();
    }

    public void testIsNullThrowsExceptionIfArgumentIsNotNullWithMessage() {
        new AssertThrows(IllegalArgumentException.class, "object is not null") {
            public void test() throws Exception {
                Assert.isNull(new Object(), "Bla");
            }

            protected void checkExceptionExpectations(Exception actualException) {
                assertEquals("Bla", actualException.getMessage());
                super.checkExceptionExpectations(actualException);
            }
        }.runTest();
    }

    public void testIsTrueWithFalseExpressionThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.isTrue(false);
            }
        }.runTest();
    }

    public void testIsTrueWithTrueExpressionSunnyDay() throws Exception {
        Assert.isTrue(true);
    }

    public void testHasLengthWithNullStringThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.hasLength(null);
            }
        }.runTest();
    }

    public void testHasLengthWithEmptyStringThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.hasLength("");
            }
        }.runTest();
    }

    public void testHasLengthWithWhitespaceOnlyStringDoesNotThrowException() throws Exception {
        Assert.hasLength("\t  ");
    }

    public void testHasLengthSunnyDay() throws Exception {
        Assert.hasLength("I Heart ...");
    }

    public void testDoesNotContainWithNullSearchStringDoesNotThrowException() throws Exception {
        Assert.doesNotContain(null, "rod");
    }

    public void testDoesNotContainWithNullSubstringDoesNotThrowException() throws Exception {
        Assert.doesNotContain("A cool chick's name is Brod. ", null);
    }

    public void testDoesNotContainWithEmptySubstringDoesNotThrowException() throws Exception {
        Assert.doesNotContain("A cool chick's name is Brod. ", "");
    }

    public void testAssertNotEmptyWithNullCollectionThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.notEmpty((Collection) null);
            }
        }.runTest();
    }

    public void testAssertNotEmptyWithEmptyCollectionThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.notEmpty(new ArrayList());
            }
        }.runTest();
    }

    public void testAssertNotEmptyWithCollectionSunnyDay() throws Exception {
        List collection = new ArrayList();
        collection.add("");
        Assert.notEmpty(collection);
    }

    public void testAssertNotEmptyWithNullMapThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.notEmpty((Map) null);
            }
        }.runTest();
    }

    public void testAssertNotEmptyWithEmptyMapThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.notEmpty(new HashMap());
            }
        }.runTest();
    }

    public void testAssertNotEmptyWithMapSunnyDay() throws Exception {
        Map map = new HashMap();
        map.put("", "");
        Assert.notEmpty(map);
    }

    public void testIsInstanceofClassWithNullInstanceThrowsException() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                Assert.isInstanceOf(String.class, null);
            }
        }.runTest();
    }

    public void testStateWithFalseExpressionThrowsException() throws Exception {
        new AssertThrows(IllegalStateException.class) {
            public void test() throws Exception {
                Assert.state(false);
            }
        }.runTest();
    }

    public void testStateWithTrueExpressionSunnyDay() throws Exception {
        Assert.state(true);
    }

}
