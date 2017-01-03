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
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.declarative.Design;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO class description
 * TODO make this like utility
 * TODO link fields to superclass
 * TODO Grid special handling
 * TODO try dateField
 *
 * @author Vaadin Ltd
 */
public class DesignToJavaConverter {


    public static void convertDeclarativeToJava(String packageName,
                                                String className,
                                                String baseClassName,
                                                String baseClassText,
                                                InputStream inputHtml, OutputStream output) throws Exception {

        JCodeModel jCodeModel = new JCodeModel();
        JPackage jPackage = jCodeModel._package(packageName);
        JDefinedClass declarativeClass = jPackage._class(className);
        Component rootComponent = null;
        if(baseClassText!=null)
        {
            Map<String,String> fields = collectFields(baseClassText);
            rootComponent = createComponent(fields);
        }
        if (baseClassName != null) {
            declarativeClass._extends(jCodeModel.directClass(baseClassName));
        }
        JMethod init = declarativeClass.method(Modifier.PUBLIC + Modifier.STATIC, void.class, "init");
        Design.setComponentFactory(new SpyComponentFactory(jCodeModel, declarativeClass,init.body()));
        Design.read(inputHtml, rootComponent);
        jCodeModel.build(new MyCodeWriter(output));
    }

    private static Component createComponent(Map<String, String> fields) {
        return null;//todo
    }

    private static Map<String, String> collectFields(String baseClassText) {
        Pattern pattern = Pattern.compile(".*\\{(\\s+protected\\s+[^\\s]+\\s+([^\\s;]+)\\s*;)*.*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(baseClassText);
        while (matcher.find()) {
            System.out.println("matcher.group(2) = " + matcher.group(2));
        }
        return null;
    }

    private static class MyCodeWriter extends CodeWriter {
        private final OutputStream output;

        public MyCodeWriter(OutputStream output) {
            this.output = output;
        }

        @Override
        public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
            return output;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
