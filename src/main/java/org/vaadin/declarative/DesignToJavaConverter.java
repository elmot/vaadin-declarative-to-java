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
import com.vaadin.ui.declarative.Design;

import java.io.*;
import java.lang.reflect.Modifier;

/**
 * TODO link fields to superclass
 * TODO Grid special handling
 *
 * @author Vaadin Ltd
 */
public class DesignToJavaConverter {


    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 == 1) {
            printUsageAndExit();
        }
        String packageName = "";
        String className = "Example";
        String baseClassName = null;
        InputStream input = System.in;
        OutputStream output = System.out;

        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-p":
                    packageName = args[i + 1];
                    break;
                case "-c":
                    className = args[i + 1];
                    break;
                case "-b":
                    baseClassName = args[i + 1];
                    break;
                case "-s":
                    input = new BufferedInputStream(new FileInputStream(args[i + 1]));
                    break;
                case "-o":
                    output = new BufferedOutputStream(new FileOutputStream(args[i + 1]));
                    break;
                default:
                    printUsageAndExit();
            }
        }
        convertDeclarativeToJava(packageName, className, baseClassName, input, output);
    }

    private static void printUsageAndExit() {
        System.out.println("Usage:");
        System.out.println("    java " + DesignToJavaConverter.class.getName() + " <options>");
        System.out.println("Options:");
        System.out.println("    -p packageName");
        System.out.println("    -c className");
        System.out.println("    -b baseClassName");
        System.out.println("    -s sourceFilePath");
        System.exit(1);
    }

    public static void convertDeclarativeToJava(String packageName,
                                                String className,
                                                String baseClassName,
                                                InputStream input, OutputStream output) throws Exception {

        JCodeModel jCodeModel = new JCodeModel();
        JPackage jPackage = jCodeModel._package(packageName);
        JDefinedClass declarativeClass = jPackage._class(className);
        if (baseClassName != null) {
            declarativeClass._extends(jCodeModel.directClass(baseClassName));
        }
        JMethod init = declarativeClass.method(Modifier.PUBLIC + Modifier.STATIC, void.class, "init");
        Design.setComponentFactory(new SpyComponentFactory(jCodeModel, declarativeClass, init.body()));
        Design.read(input);
        jCodeModel.build(new MyCodeWriter(output));
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
