Vaadin Declarative to pure Java Converter
===

Some unfinished experiments with CGLIB and Vaadin 7. Works also against Vaadin 8, thanks to @johannest for testing.

Vaadin declarative code is read, then pure Vaadin java initialization is generated out from it.
Not all the cases are covered, for instance, Grid does not work properly.

```html
<!DOCTYPE html>
<html>
 <head>
  <meta name="package-mapping" content="my:com.addon.mypackage"/>
 </head>
 <body>
  <vaadin-vertical-layout width="500px">
   <vaadin-horizontal-layout>
    <vaadin-label plain-text caption="FooBar"></vaadin-label>
    <vaadin-native-button _id=firstButton>Native click me</vaadin-native-button>
	<vaadin-native-button id = secondButton _id="localID">Another button</vaadin-native-button>
	<vaadin-native-button>Yet another button</vaadin-native-button>
    <vaadin-button plain-text width = "150px">Click me</vaadin-button>
   </vaadin-horizontal-layout>
   <vaadin-text-field caption = "Text input"/>
   <vaadin-password-field  caption = "Password field" height="200px" width="300px"/>
  </vaadin-vertical-layout>
 </body>
</html>
```

is converted into
```java

package org.vaadin.example;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class Example
    extends VerticalLayout
{


    public final void init() {
        this.setWidth("500px");
        this.setMargin(new MarginInfo(false, false, false, false));
        HorizontalLayout horizontalLayout1 = new HorizontalLayout();
        horizontalLayout1 .setMargin(new MarginInfo(false, false, false, false));
        Label label1 = new Label();
        label1 .setCaption("FooBar");
        label1 .setContentMode(ContentMode.TEXT);
        horizontalLayout1 .addComponent(label1);
        horizontalLayout1 .setComponentAlignment(label1, Alignment.TOP_LEFT);
        NativeButton nativeButton1 = new NativeButton();
        nativeButton1 .setHtmlContentAllowed(true);
        nativeButton1 .setCaption("Native click me");
        horizontalLayout1 .addComponent(nativeButton1);
        horizontalLayout1 .setComponentAlignment(nativeButton1, Alignment.TOP_LEFT);
        NativeButton nativeButton2 = new NativeButton();
        nativeButton2 .setId("secondButton");
        nativeButton2 .setHtmlContentAllowed(true);
        nativeButton2 .setCaption("Another button");
        nativeButton2 .setId("secondButton");
        horizontalLayout1 .addComponent(nativeButton2);
        horizontalLayout1 .setComponentAlignment(nativeButton2, Alignment.TOP_LEFT);
        NativeButton nativeButton3 = new NativeButton();
        nativeButton3 .setHtmlContentAllowed(true);
        nativeButton3 .setCaption("Yet another button");
        horizontalLayout1 .addComponent(nativeButton3);
        horizontalLayout1 .setComponentAlignment(nativeButton3, Alignment.TOP_LEFT);
        Button button1 = new Button();
        button1 .setWidth("150px");
        button1 .setCaption("Click me");
        horizontalLayout1 .addComponent(button1);
        horizontalLayout1 .setComponentAlignment(button1, Alignment.TOP_LEFT);
        this.addComponent(horizontalLayout1);
        this.setComponentAlignment(horizontalLayout1, Alignment.TOP_LEFT);
        TextField textField1 = new TextField();
        textField1 .setCaption("Text input");
        this.addComponent(textField1);
        this.setComponentAlignment(textField1, Alignment.TOP_LEFT);
        PasswordField passwordField1 = new PasswordField();
        passwordField1 .setCaption("Password field");
        passwordField1 .setWidth("300px");
        passwordField1 .setHeight("200px");
        this.addComponent(passwordField1);
        this.setComponentAlignment(passwordField1, Alignment.TOP_LEFT);
    }

}

```

How to use DesignToJavaConverter in your project
===
1. Include following dependencies in your pom.xml

```xml
	<dependency>
		<groupId>com.sun.codemodel</groupId>
		<artifactId>codemodel</artifactId>
		<version>2.6</version>
	</dependency>
	<dependency>
		<groupId>junit</groupId>
		<artifactId>junit</artifactId>
		<version>4.12</version>
	</dependency>

	<dependency>
		<groupId>cglib</groupId>
		<artifactId>cglib-nodep</artifactId>
		<version>2.2</version>
	</dependency>
```

2. Also make sure that `javax.servlet-api` is available (provided scope is not enough)

3. Add following Java files to your project:
    
    `DesignToJavaConverter.java`, 
    `SkipCodePartException.java`, and
    `SpyComponentFactory.java`

4. If your design uses custom components, make sure that each custom component has a default constructor.

5. Run `java declarative.DesignToJavaConverter -b base.package.name -s path/to/declarative/file/MyViewDesign.html`

6. Take console output and create a new Java (e.g. `MyViewJava.java`) class corresponding your `MyViewDesign.java` class

7. Clean up the Java class (`MyViewJava.java`)
	- fix possible autowired thing
	- check the warnings (e.g. "Warning! Unsupported class...") from console output and fix in the code if needed
	- make sure both `MyViewJava.java` and `MyViewDesign.java` extend same (layout) class
	- refactor the content
	- fix numeric literals
	- compare the class to your MyViewDesign.java class, and refactor appropriate local variables to fields
	- create constructor where the generated init method is called

8. Use `MyViewJava.java` instead of `MyViewDesign.java`
