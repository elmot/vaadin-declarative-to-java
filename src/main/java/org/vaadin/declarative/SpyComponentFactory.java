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

import com.sun.codemodel.*;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.declarative.DesignContext;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Generates instrumented components to store java sources
 *
 * @author https://github.com/elmot
 */
public class SpyComponentFactory implements Design.ComponentFactory {

    private final Map<String, BiConsumer<Consumer<JExpression>, Object>> clazzPramHandlers = new HashMap<>();

    private final Map<Alignment, String> alignments = new HashMap<>();

    private void addHandler(BiConsumer<Consumer<JExpression>, Object> handler, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            clazzPramHandlers.put(clazz.getName(), handler);
        }
    }

    private void initHandlers() {
        addHandler((Consumer<JExpression> expr, Object obj) -> expr.accept(JExpr.lit(((Number) obj).longValue())),
                byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> expr.accept(JExpr.lit(((Number) obj).doubleValue())),
                float.class, Float.class, double.class, Double.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> expr.accept(JExpr.lit(((Character) obj).charValue())),
                char.class, Character.class);

        addHandler(
                (Consumer<JExpression> expr, Object obj) -> expr.accept(JExpr.lit(((String) obj))),
                String.class, CharSequence.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> expr.accept(JExpr.lit(((Boolean) obj))),
                boolean.class, Boolean.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> {
            @SuppressWarnings("RedundantCast")
            String constName = alignments.get((Alignment) obj);
            expr.accept(((JClass) jCodeModel._ref(Alignment.class)).staticRef(constName));
        }, Alignment.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> {
            JClass jClass = (JClass) jCodeModel._ref((Class<?>) obj);
            expr.accept(jClass.dotclass());
        }, Class.class);

        addHandler((Consumer<JExpression> expr, Object obj) -> {
            @SuppressWarnings("RedundantCast")
            MarginInfo marginInfo = (MarginInfo) obj;
            expr.accept(JExpr._new(jCodeModel._ref(MarginInfo.class))
                    .arg(JExpr.lit(marginInfo.hasTop()))
                    .arg(JExpr.lit(marginInfo.hasRight()))
                    .arg(JExpr.lit(marginInfo.hasBottom()))
                    .arg(JExpr.lit(marginInfo.hasLeft()))
            );
        }, MarginInfo.class);


        for (Field field : Alignment.class.getDeclaredFields()) {
            if (Alignment.class.equals(field.getType()) && (field.getModifiers() & Modifier.STATIC) != 0) {
                try {
                    alignments.put((Alignment) field.get(null), field.getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Map<String, Enhancer> enhancers = new HashMap<>();
    private final JBlock body;
    private final JDefinedClass rootClass;
    private final JCodeModel jCodeModel;

    private Map<Object, JExpression> variables = new HashMap<>();
    private Set<String> usedNames = new HashSet<>();
    private boolean skipLogging = false;
    private boolean expectRoot = true;

    public SpyComponentFactory(JCodeModel jCodeModel, JDefinedClass rootClass, JBlock body) {
        this.rootClass = rootClass;
        this.body = body;
        this.jCodeModel = jCodeModel;
        initHandlers();
    }

    @Override
    public Component createComponent(String fullyQualifiedClassName, DesignContext context) {
        try {
            Class<? extends Component> aClass = DesignToJavaConverter.class.getClassLoader().loadClass(fullyQualifiedClassName).asSubclass(Component.class);
            Enhancer enhancer = enhancers.get(fullyQualifiedClassName);
            if (enhancer == null) {
                enhancer = new Enhancer();
                enhancer.setSuperclass(aClass);
                enhancer.setCallback(new MyMethodInterceptor());
                enhancers.put(fullyQualifiedClassName, enhancer);
            }

            boolean oldSkipLogging = skipLogging;
            skipLogging = true;
            try {
                Component component = (Component) enhancer.create();
                String baseName = aClass.getSimpleName();
                JType jType = jCodeModel._ref(aClass);
                if (expectRoot) {
                    expectRoot = false;
                    variables.put(component,JExpr._this());
                    if(rootClass._extends()==jCodeModel.ref(Object.class)) {
                        rootClass._extends((JClass) jType);
                    }
                } else {
                    baseName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
                    int i = 1;
                    String name;
                    do {
                        name = baseName + i++;
                    } while (usedNames.contains(name));
                    usedNames.add(name);
                    JVar decl = body.decl(jType, name, JExpr._new(jType));
                    variables.put(component, decl);
                }
                return component;
            } finally {
                skipLogging = oldSkipLogging;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class MyMethodInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            boolean oldSkipLogging = skipLogging;
            if (!skipLogging) {
                String name = method.getName();
                if (name.startsWith("set") || name.startsWith("add")) {
                    if (Modifier.isPublic(method.getModifiers())) {
                        JExpression jVar = variables.get(obj);
                        JInvocation invoke = jVar.invoke(name);
                        try {
                            makeParams(method.getParameters(), args, invoke::arg);
                            body.add(invoke);
                        } catch (SkipCodePartException ignored) {
                        }
                        skipLogging = true;

                    } else {
                        System.err.println("Warning! non-public method call detected");
                        printExceptionCodePoint();
                    }
                }
            }
            try {
                return proxy.invokeSuper(obj, args);
            } finally {
                skipLogging = oldSkipLogging;
            }
        }
    }

    private void makeParams(Parameter[] parameters, Object[] args, Consumer<JExpression> expr)
            throws SkipCodePartException {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> argType = arg == null ? null : arg.getClass();
            JExpression varRef = variables.get(arg);
            if (arg == null) {
                expr.accept(JExpr._null());
            } else if (varRef != null) {
                expr.accept(varRef);
            } else if (argType.isEnum()) {
                expr.accept(((JClass) jCodeModel._ref(argType)).staticRef(((Enum) arg).name()));
            } else if (argType.isArray()) {
                if (parameters != null && parameters[i].isVarArgs()) {
                    makeParams(null, (Object[]) arg, expr);
                } else {
                    JArray jArray = JExpr.newArray(jCodeModel.ref(argType.getComponentType()));
                    expr.accept(jArray);
                    makeParams(null, (Object[]) arg, jArray::add);
                }
            } else {
                BiConsumer<Consumer<JExpression>, Object> paramWorker = clazzPramHandlers.get(argType.getName());
                if (paramWorker != null) {
                    paramWorker.accept(expr, arg);
                } else {
                    System.err.println("Warning! Unsupported class " + argType);
                    printExceptionCodePoint();
                    throw new SkipCodePartException();
                }
            }
        }
    }

    private void printExceptionCodePoint() {
        RuntimeException runtimeException = new RuntimeException();
        StackTraceElement[] stackTrace = runtimeException.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();
            if (className.startsWith("com.vaadin.ui") && stackTraceElement.getLineNumber() > 0) {
                System.err.println("at " + stackTraceElement);
                break;
            }
        }
    }
}
