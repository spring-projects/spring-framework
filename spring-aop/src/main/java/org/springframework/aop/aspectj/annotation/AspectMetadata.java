/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Metadata for an AspectJ aspect class, with an additional Spring AOP pointcut
 * for the per clause.
 *
 * <p>Uses AspectJ 5 AJType reflection API, enabling us to work with different
 * AspectJ instantiation models such as "singleton", "pertarget" and "perthis".
 *
 * <p>元数据方面(AspectMetadata)
 * <p>AspectJ方面类的元数据，带有额外的Spring AOP切入点对于每一条款。
 * <p>使用AspectJ 5 AJType反射API，使我们能够使用不同的AspectJ实例化模型,
 * 如“singleton”、“pertarget”和“perthis”。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.aspectj.AspectJExpressionPointcut
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

	/**
	 * The name of this aspect as defined to Spring (the bean name) -
	 * allows us to determine if two pieces of advice come from the
	 * same aspect and hence their relative precedence.
	 * <p>此方面的名称定义为Spring（bean名称）-
	 * <p>允许我们确定两条增强器是否来自相同的方面，因此它们的相对优先级
	 */
	private final String aspectName;

	/**
	 * The aspect class, stored separately for re-resolution of the
	 * corresponding AjType on deserialization.
	 * <p>切面类, 单独存储以重新解析反序列化对应的AspectJType
	 */
	private final Class<?> aspectClass;

	/**
	 * AspectJ reflection information.
	 * <p>Re-resolved on deserialization since it isn't serializable itself.
	 * <p>AspectJ反射信息.
	 * <p>由于反序列化本身不可序列化, 因此重新解决了反序列化问题
	 */
	private transient AjType<?> ajType;

	/**
	 * Spring AOP pointcut corresponding to the per clause of the
	 * aspect. Will be the {@code Pointcut.TRUE} canonical instance in the
	 * case of a singleton, otherwise an AspectJExpressionPointcut.
	 * <p>Spring AOP切入点对应于切面
	 * <p>将是{@code Pointcut.TRUE}规范实例如果是单例，则为AspectJ表达式切入点。
	 */
	private final Pointcut perClausePointcut;


	/**
	 * Create a new AspectMetadata instance for the given aspect class.
	 * <p>为给定的切面类创建一个新的AspectJ元数据实例
	 *
	 * @param aspectClass the aspect class
	 * @param aspectName  the name of the aspect
	 */
	public AspectMetadata(Class<?> aspectClass, String aspectName) {
		this.aspectName = aspectName;

		Class<?> currClass = aspectClass;
		AjType<?> ajType = null;
		while (currClass != Object.class) {
			AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
			if (ajTypeToCheck.isAspect()) {
				ajType = ajTypeToCheck;
				break;
			}
			currClass = currClass.getSuperclass();
		}
		if (ajType == null) {
			throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
		}
		if (ajType.getDeclarePrecedence().length > 0) {
			throw new IllegalArgumentException("DeclarePrecedence not presently supported in Spring AOP");
		}
		this.aspectClass = ajType.getJavaClass();
		this.ajType = ajType;

		switch (this.ajType.getPerClause().getKind()) {
			case SINGLETON:
				this.perClausePointcut = Pointcut.TRUE;
				return;
			case PERTARGET:
			case PERTHIS:
				AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
				ajexp.setLocation(aspectClass.getName());
				ajexp.setExpression(findPerClause(aspectClass));
				ajexp.setPointcutDeclarationScope(aspectClass);
				this.perClausePointcut = ajexp;
				return;
			case PERTYPEWITHIN:
				// Works with a type pattern
				this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
				return;
			default:
				throw new AopConfigException(
						"PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
		}
	}

	/**
	 * Extract contents from String of form {@code pertarget(contents)}.
	 */
	private String findPerClause(Class<?> aspectClass) {
		String str = aspectClass.getAnnotation(Aspect.class).value();
		int beginIndex = str.indexOf('(') + 1;
		int endIndex = str.length() - 1;
		return str.substring(beginIndex, endIndex);
	}


	/**
	 * Return AspectJ reflection information.
	 */
	public AjType<?> getAjType() {
		return this.ajType;
	}

	/**
	 * Return the aspect class.
	 */
	public Class<?> getAspectClass() {
		return this.aspectClass;
	}

	/**
	 * Return the aspect name.
	 */
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * Return a Spring pointcut expression for a singleton aspect.
	 * (e.g. {@code Pointcut.TRUE} if it's a singleton).
	 */
	public Pointcut getPerClausePointcut() {
		return this.perClausePointcut;
	}

	/**
	 * Return whether the aspect is defined as "perthis" or "pertarget".
	 */
	public boolean isPerThisOrPerTarget() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
	}

	/**
	 * Return whether the aspect is defined as "pertypewithin".
	 */
	public boolean isPerTypeWithin() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTYPEWITHIN);
	}

	/**
	 * Return whether the aspect needs to be lazily instantiated.
	 */
	public boolean isLazilyInstantiated() {
		return (isPerThisOrPerTarget() || isPerTypeWithin());
	}


	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.ajType = AjTypeSystem.getAjType(this.aspectClass);
	}

}
