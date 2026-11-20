/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
