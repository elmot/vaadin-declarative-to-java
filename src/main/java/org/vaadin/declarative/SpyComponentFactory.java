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

import com.vaadin.ui.Component;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.declarative.DesignContext;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;

/**
 * TODO class description
 *
 * @author Vaadin Ltd
 */
public class SpyComponentFactory implements Design.ComponentFactory {

    private Design.ComponentFactory defFactory = new Design.DefaultComponentFactory();

    public Component createComponent(String fullyQualifiedClassName, DesignContext context) {
        return defFactory.createComponent(fullyQualifiedClassName, context);
    }
}
