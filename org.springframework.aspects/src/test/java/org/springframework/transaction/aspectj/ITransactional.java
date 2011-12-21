package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ITransactional {
	
	Object echo(Throwable t) throws Throwable;

}
