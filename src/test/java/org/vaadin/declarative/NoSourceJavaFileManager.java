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

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JavaFileManager without source classes
 *
 * @author https://github.com/elmot
 */
class NoSourceJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    public NoSourceJavaFileManager(StandardJavaFileManager standardFileManager) {
        super(standardFileManager);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if (kinds.contains(JavaFileObject.Kind.SOURCE)) {
            kinds = new HashSet<>(kinds);
            kinds.remove(JavaFileObject.Kind.SOURCE);
        }
        return super.list(location, packageName, kinds, recurse);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return super.getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return new NullJavaFileObject(kind, className, className);
    }
}
