/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.declarative;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.declarative.Design;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO class description
 *
 * @author Vaadin Ltd
 */
public class DesignToJavaConverter {


    private static JMethod init;
    private static Map<Object, JVar> variables;
    private static Set<String> usedNames;
    private static JCodeModel jCodeModel;

    public static void convertDeclarativeToJava(String packageName,
                                                String className,
                                                InputStream input, OutputStream output) throws Exception {

        final ClassPool classPool = ClassPool.getDefault();
        final Loader loader = new Loader(classPool);

        loader.addTranslator(classPool, new MyTranslator(loader));

        Method doConvertDeclarativeToJava = loader.loadClass(DesignToJavaConverter.class.getName())
                .getDeclaredMethod("doConvertDeclarativeToJava"
                        , String.class, String.class, InputStream.class, OutputStream.class);
        doConvertDeclarativeToJava.invoke(null, packageName, className, input, output);
    }

    public static void doConvertDeclarativeToJava(String packageName,
                                                  String className,
                                                  InputStream input, OutputStream output) throws Exception {

        variables = new IdentityHashMap<>();
        usedNames = new HashSet<>();
        jCodeModel = new JCodeModel();

        JPackage jPackage = jCodeModel._package(packageName);
        JDefinedClass declarativeClass = jPackage._class(className);
        declarativeClass._extends(Panel.class);
        init = declarativeClass.method(JMod.PUBLIC, Void.TYPE, "init");

        Design.read(input);

        jCodeModel.build(new SingleStreamCodeWriter(output));
    }

    private static class MyTranslator implements Translator {

        private final Loader loader;
        private CtClass componentClass;

        public MyTranslator(Loader loader) {
            this.loader = loader;
        }

        public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
            componentClass = pool.get(Component.class.getName());
        }

        public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
            CtClass ctClass = pool.get(classname);
            if (!ctClass.isInterface() && ctClass.subtypeOf(componentClass) && (ctClass.getModifiers() & Modifier.ABSTRACT) == 0) {
                try {
                    CtConstructor declaredConstructor = ctClass.getDeclaredConstructor(null);
                    declaredConstructor.insertBeforeBody(
                            DesignToJavaConverter.class.getName() + ".registerInstance(" + ctClass.getName() + ".class, this);"
                    );
                } catch (NotFoundException ignored) {
                }
            }
        }
    }

    public static void registerInstance(Class declaredClass, Object obj) {
        if (declaredClass.equals(obj.getClass())) {
            String name = declaredClass.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            int i = 1;
            for (; usedNames.contains(name + i); i++) ;
            name = name + i;
            usedNames.add(name);
            JType jType = jCodeModel._ref(declaredClass);

            init.body().decl(jType, name, JExpr._new(jType));
        }
    }
}
