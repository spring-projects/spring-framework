package org.springframework.core.annotation.alisfors;

import org.junit.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;

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
        Test1 annotation2 = AnnotatedElementUtils.getMergedAnnotation(Element2.class, Test1.class);
        assertEquals(annotation, annotation2);
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
        Test4 annotation = AnnotatedElementUtils.getMergedAnnotation(Element3.class, Test4.class);
        String test1 = annotation.test1();
        String test2 = annotation.test2();
        String test3 = annotation.test3();
        assertEquals("override the method", test1);
        assertEquals(test1, test2);
        assertEquals(test1, test3);
        assertEquals(test2, test3);
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

        @AliasFor("test2")
        @AliasFor("test3")
        String test1() default "test2";

        @AliasFor("test1")
        @AliasFor("test3")
        String test2() default "test2";

        @AliasFor(annotation = Test5.class)
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
        Test5 test5 = AnnotatedElementUtils.getMergedAnnotation(Element4.class, Test5.class);
        Test6 test6 = AnnotatedElementUtils.getMergedAnnotation(Element4.class, Test6.class);
        System.out.println(test5.toString());
        System.out.println(test6.toString());
        assertEquals("override the method", test6.test1());
        assertEquals("override the method", test6.test2());
        assertEquals("override the method", test6.test3());
        assertEquals("override the method", test5.test1());
        assertEquals("override the method", test5.test2());
        assertEquals("override the method", test5.test3());
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Test8 {

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
    @Test8
    public @interface Test9 {

        @AliasFor(annotation = Test8.class)
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
    @Test9
    public @interface Test10 {

        @AliasFor(annotation = Test9.class)
        String test3() default "test3";
    }

    @Test10(test3 = "override the method")
    public static class Element5 {
    }

    @Test
    public void test4() {
        // not pass, does it matter? is it a bug?
        // Mr Sam Brannen, confirm this question, please
        // if it is a bug, please contact 1936978077@qq.com by email
        Test8 test8 = AnnotatedElementUtils.getMergedAnnotation(Element5.class, Test8.class);
        Test9 test9 = AnnotatedElementUtils.getMergedAnnotation(Element5.class, Test9.class);
        System.out.println(test8.toString());
        System.out.println(test9.toString());
        assertEquals("override the method", test9.test1());
        assertEquals("override the method", test9.test2());
        assertEquals("override the method", test9.test3());
        assertEquals("override the method", test8.test1());
        assertEquals("override the method", test8.test2());
        assertEquals("override the method", test8.test3());
    }
}
