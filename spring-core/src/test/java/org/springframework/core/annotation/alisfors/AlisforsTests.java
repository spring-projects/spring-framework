package org.springframework.core.annotation.alisfors;

import org.junit.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Zicheng Zhang
 * @date 2020/08/08
 */
public class AlisforsTests {

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Test1 {
        String test1() default "test1";
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Test2 {
        String test2() default "test2";
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Test1
    @Test2
    public @interface Test3 {

        @AliasFor(annotation = Test1.class, attribute = "test1")
        @AliasFor(annotation = Test2.class, attribute = "test2")
        String test3() default "test3";
    }

    @Test3(test3 = "override the method")
    public static class Element1 {
    }

    @Test1(test1 = "override the method")
    public static class Element2 {
    }

    @Test
    public void test1() {
        Test1 annotation = AnnotatedElementUtils.getMergedAnnotation(Element1.class, Test1.class);
        Test3 test3 = AnnotatedElementUtils.getMergedAnnotation(Element1.class, Test3.class);
        Test1 annotation2 = AnnotatedElementUtils.getMergedAnnotation(Element2.class, Test1.class);
        System.out.println(test3.toString());
        System.out.println(annotation.hashCode());
        System.out.println(annotation.toString());
        System.out.println(annotation.equals(annotation2));
        System.out.println(annotation.annotationType());
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Test4 {

        @AliasFor("test2")
        @AliasFor("test3")
        String test1() default "test";

        @AliasFor("test1")
        @AliasFor("test3")
        String test2() default "test";

        @AliasFor("test1")
        @AliasFor("test2")
        String test3() default "test";
    }

    @Test4(test1 = "override the method")
    public static class Element3 {
    }

    @Test
    public void test2() {
        Test4 annotation2 = AnnotatedElementUtils.getMergedAnnotation(Element3.class, Test4.class);
        System.out.println("test1->" + annotation2.test1());
        System.out.println("test2->" + annotation2.test2());
        System.out.println("test3->" + annotation2.test3());
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Test5 {

        @AliasFor("test2")
        @AliasFor("test3")
        String test1() default "test1";

        @AliasFor("test1")
        @AliasFor("test3")
        String test2() default "test1";

        @AliasFor("test1")
        @AliasFor("test2")
        String test3() default "test1";
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Test5
    public @interface Test6 {

        @AliasFor(annotation = Test5.class)
        @AliasFor("test2")
        @AliasFor("test3")
        String test1() default "test2";

        @AliasFor("test1")
        @AliasFor("test3")
        String test2() default "test2";

        @AliasFor("test1")
        @AliasFor("test2")
        String test3() default "test2";
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Test6
    public @interface Test7 {

        @AliasFor(annotation = Test6.class)
        String test3() default "test3";
    }

    @Test7(test3 = "override the method")
    public static class Element4 {
    }

    @Test
    public void test3() {
        Test5 test1 = AnnotatedElementUtils.getMergedAnnotation(Element4.class, Test5.class);
        Test6 test2 = AnnotatedElementUtils.getMergedAnnotation(Element4.class, Test6.class);
        System.out.println("test1->" + test1.toString());
        System.out.println("test2->" + test2.toString());
    }
}
