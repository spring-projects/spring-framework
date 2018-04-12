package org.springframework.test.context.junit4.spr16716;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

/**
 * Validate SpringFailOnTimeout contract
 * <a href="https://jira.spring.io/browse/SPR-16716" target="_blank">SPR-16716</a>.
 *
 * @author Igor Suhorukov
 * @since 5.0.6
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringFailOnTimeoutExceptionTest {

	@Mock
	private Statement statement;

	@Test(expected = IllegalArgumentException.class)
	public void validateOriginalExceptionFromEvaluateMethod() throws Throwable {
		IllegalArgumentException expectedException = new IllegalArgumentException();
		doThrow(expectedException).when(statement).evaluate();
		new SpringFailOnTimeout(statement, TimeUnit.SECONDS.toMillis(1)).evaluate();
	}

	@Test(expected = TimeoutException.class)
	public void validateTimeoutException() throws Throwable {
		doAnswer((Answer<Void>) invocation -> {
			TimeUnit.MILLISECONDS.sleep(50);
			return null;
		}).when(statement).evaluate();
		new SpringFailOnTimeout(statement, TimeUnit.MILLISECONDS.toMillis(1)).evaluate();
	}
}
