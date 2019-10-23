/*
 * Copyright 2002-2019 the original author or authors.
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

package example.type;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link org.springframework.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.core.type.AssignableTypeFilterTests
 */
public class AssignableTypeFilterTestsTypes {

	public static class TestNonInheritingClass {
	}

	public interface TestInterface {
	}

	public static class TestInterfaceImpl implements TestInterface {
	}

	public interface SomeDaoLikeInterface {
	}

	public static class SomeDaoLikeImpl extends SimpleJdbcDaoSupport implements SomeDaoLikeInterface {
	}

	public interface JdbcDaoSupport {
	}

	public static class SimpleJdbcDaoSupport implements JdbcDaoSupport {
	}

}
