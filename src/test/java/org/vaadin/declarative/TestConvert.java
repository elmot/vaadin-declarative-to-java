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

import org.junit.Assert;
import org.junit.Test;

import javax.tools.DiagnosticListener;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;

/**
 * TODO class description
 *
 * @author Vaadin Ltd
 */
public class TestConvert {

    public static final String PACKAGE_NAME = "org.vaadin.example";
    public static final String CLASS_NAME = "Example";

    @Test
    public void testSimple() throws Exception {
        unTest("testFile.html",null);
    }

    @Test
    public void testGrid() throws Exception {
        unTest("TestTest.html","org.vaadin.declarative.TestTest");
    }

    public void unTest(String name, String baseClass) throws Exception {
        InputStream htmlSource = TestConvert.class.getResourceAsStream(name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DesignToJavaConverter.convertDeclarativeToJava(PACKAGE_NAME, CLASS_NAME, baseClass,
                htmlSource, baos);
        System.out.write(baos.toByteArray());

        JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticListener<JavaFileObject> javaFileObjectDiagnosticListener = diagnostic -> System.out.println("diagnostic = " + diagnostic);
        StandardJavaFileManager standardFileManager = systemJavaCompiler.getStandardFileManager(javaFileObjectDiagnosticListener, null, null);

        StringWriter compilerLog = new StringWriter();

        JavaFileObject fileObject = new ReadOnlyJavaFileObject(baos);

        ForwardingJavaFileManager<StandardJavaFileManager> fileManager = new NoSourceJavaFileManager(standardFileManager);

        JavaCompiler.CompilationTask task = systemJavaCompiler.getTask(compilerLog, fileManager, javaFileObjectDiagnosticListener, null, null, Collections.singleton(fileObject));

        Assert.assertTrue(task.call());
    }

}
