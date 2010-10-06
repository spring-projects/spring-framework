package org.springframework.scheduling;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * Abstract aspect that routes selected methods asynchronously.
 * <p>
 * This aspect, by default, uses {@link SimpleAsyncTaskExecutor} to route method
 * execution. However, you may inject it with any implementation of 
 * {@link Executor} to override the default.
 * 
 * @author Ramnivas Laddad
 */
public abstract aspect AbstractAsynchronousExecutionAspect {
	private AsyncTaskExecutor asyncExecutor;
	
	public AbstractAsynchronousExecutionAspect() {
		// Set default executor, which may be replaced by calling setExecutor(Executor)
		setExecutor(new SimpleAsyncTaskExecutor());
	}

    public abstract pointcut asyncMethod();

    Object around() : asyncMethod() {
    		Callable<Object> callable = new Callable<Object>() {
    			public Object call() throws Exception {
    				Object result = proceed();
    				if (result instanceof Future) {
    					return ((Future<?>) result).get();
    				}
    				return null;
    			}};
    			
    		Future<?> result = asyncExecutor.submit(callable);
		
    		if (Future.class.isAssignableFrom(((MethodSignature)thisJoinPointStaticPart.getSignature()).getReturnType())) {
			return result;
		} else {
			return null;
		}
    }

    public void setExecutor(Executor executor) {
    		if (executor instanceof AsyncTaskExecutor) {
        		this.asyncExecutor = (AsyncTaskExecutor) executor;
    		} else {
    			this.asyncExecutor = new TaskExecutorAdapter(asyncExecutor);
    		}
    }
    
}
