/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.proxy;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassesKey;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ReflectUtils;

/**
 * <code>Mixin</code> allows
 * multiple objects to be combined into a single larger object. The
 * methods in the generated object simply call the original methods in the
 * underlying "delegate" objects.
 * @author Chris Nokleberg
 * @version $Id: Mixin.java,v 1.7 2005/09/27 11:42:27 baliuka Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class Mixin {
    private static final MixinKey KEY_FACTORY =
      (MixinKey)KeyFactory.create(MixinKey.class, KeyFactory.CLASS_BY_NAME);
    private static final Map ROUTE_CACHE = Collections.synchronizedMap(new HashMap());

    public static final int STYLE_INTERFACES = 0;
    public static final int STYLE_BEANS = 1;
    public static final int STYLE_EVERYTHING = 2;

    interface MixinKey {
        public Object newInstance(int style, String[] classes, int[] route);
    }

    abstract public Mixin newInstance(Object[] delegates);

    /**
     * Helper method to create an interface mixin. For finer control over the
     * generated instance, use a new instance of <code>Mixin</code>
     * instead of this static method.
     * TODO
     */
    public static Mixin create(Object[] delegates) {
        Generator gen = new Generator();
        gen.setDelegates(delegates);
        return gen.create();
    }

    /**
     * Helper method to create an interface mixin. For finer control over the
     * generated instance, use a new instance of <code>Mixin</code>
     * instead of this static method.
     * TODO
     */
    public static Mixin create(Class[] interfaces, Object[] delegates) {
        Generator gen = new Generator();
        gen.setClasses(interfaces);
        gen.setDelegates(delegates);
        return gen.create();
    }


    public static Mixin createBean(Object[] beans) {

        return createBean(null, beans);

    }
    /**
     * Helper method to create a bean mixin. For finer control over the
     * generated instance, use a new instance of <code>Mixin</code>
     * instead of this static method.
     * TODO
     */
    public static Mixin createBean(ClassLoader loader,Object[] beans) {
        Generator gen = new Generator();
        gen.setStyle(STYLE_BEANS);
        gen.setDelegates(beans);
        gen.setClassLoader(loader);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(Mixin.class.getName());

        private Class[] classes;
        private Object[] delegates;
        private int style = STYLE_INTERFACES;

        private int[] route;

        public Generator() {
            super(SOURCE);
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return classes[0].getClassLoader(); // is this right?
        }

        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(classes[0]);
        }

        public void setStyle(int style) {
            switch (style) {
            case STYLE_INTERFACES:
            case STYLE_BEANS:
            case STYLE_EVERYTHING:
                this.style = style;
                break;
            default:
                throw new IllegalArgumentException("Unknown mixin style: " + style);
            }
        }

        public void setClasses(Class[] classes) {
            this.classes = classes;
        }

        public void setDelegates(Object[] delegates) {
            this.delegates = delegates;
        }

        public Mixin create() {
            if (classes == null && delegates == null) {
                throw new IllegalStateException("Either classes or delegates must be set");
            }
            switch (style) {
            case STYLE_INTERFACES:
                if (classes == null) {
                    Route r = route(delegates);
                    classes = r.classes;
                    route = r.route;
                }
                break;
            case STYLE_BEANS:
                // fall-through
            case STYLE_EVERYTHING:
                if (classes == null) {
                    classes = ReflectUtils.getClasses(delegates);
                } else {
                    if (delegates != null) {
                        Class[] temp = ReflectUtils.getClasses(delegates);
                        if (classes.length != temp.length) {
                            throw new IllegalStateException("Specified classes are incompatible with delegates");
                        }
                        for (int i = 0; i < classes.length; i++) {
                            if (!classes[i].isAssignableFrom(temp[i])) {
                                throw new IllegalStateException("Specified class " + classes[i] + " is incompatible with delegate class " + temp[i] + " (index " + i + ")");
                            }
                        }
                    }
                }
            }
            setNamePrefix(classes[ReflectUtils.findPackageProtected(classes)].getName());

            return (Mixin)super.create(KEY_FACTORY.newInstance(style, ReflectUtils.getNames( classes ), route));
        }

        @Override
        public void generateClass(ClassVisitor v) {
            switch (style) {
            case STYLE_INTERFACES:
                new MixinEmitter(v, getClassName(), classes, route);
                break;
            case STYLE_BEANS:
                new MixinBeanEmitter(v, getClassName(), classes);
                break;
            case STYLE_EVERYTHING:
                new MixinEverythingEmitter(v, getClassName(), classes);
                break;
            }
        }

        @Override
        protected Object firstInstance(Class type) {
            return ((Mixin)ReflectUtils.newInstance(type)).newInstance(delegates);
        }

        @Override
        protected Object nextInstance(Object instance) {
            return ((Mixin)instance).newInstance(delegates);
        }
    }

    public static Class[] getClasses(Object[] delegates) {
        return route(delegates).classes.clone();
    }

//     public static int[] getRoute(Object[] delegates) {
//         return (int[])route(delegates).route.clone();
//     }

    private static Route route(Object[] delegates) {
        Object key = ClassesKey.create(delegates);
        Route route = (Route)ROUTE_CACHE.get(key);
        if (route == null) {
            ROUTE_CACHE.put(key, route = new Route(delegates));
        }
        return route;
    }

    private static class Route
    {
        private Class[] classes;
        private int[] route;

        Route(Object[] delegates) {
            Map map = new HashMap();
            ArrayList collect = new ArrayList();
            for (int i = 0; i < delegates.length; i++) {
                Class delegate = delegates[i].getClass();
                collect.clear();
                ReflectUtils.addAllInterfaces(delegate, collect);
                for (Iterator it = collect.iterator(); it.hasNext();) {
                    Class iface = (Class)it.next();
                    if (!map.containsKey(iface)) {
                        map.put(iface, i);
                    }
                }
            }
            classes = new Class[map.size()];
            route = new int[map.size()];
            int index = 0;
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                Class key = (Class)it.next();
                classes[index] = key;
                route[index] = ((Integer)map.get(key));
                index++;
            }
        }
    }
}
