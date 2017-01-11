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

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * Source file object
 *
 * @author https://github.com/elmot
 */
class ReadOnlyJavaFileObject implements JavaFileObject {
    private final ByteArrayOutputStream baos;

    ReadOnlyJavaFileObject(ByteArrayOutputStream baos) {
        this.baos = baos;
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return "Example".equals(simpleName) && kind == Kind.SOURCE;
    }

    @Override
    public NestingKind getNestingKind() {
        return NestingKind.TOP_LEVEL;
    }

    @Override
    public Modifier getAccessLevel() {
        return Modifier.PUBLIC;
    }

    @Override
    public URI toUri() {
        try {
            return new URI("virtual://" + getName());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return TestConvert.PACKAGE_NAME.replace('.', '/') + "/" + TestConvert.CLASS_NAME + ".java";
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new IOException("Read-only");
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new IOException("Read-only");
    }

    @Override
    public long getLastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean delete() {
        return false;
    }
}
