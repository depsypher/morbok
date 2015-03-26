Morbok: Extensions for [Lombok](http://projectlombok.org).
==

NOTE
--
Lombok now has @Log built in! Morbok is effectively obsoleted now, so just use Lombok instead! See this for more:
[http://projectlombok.org/features/Log.html Lombok @Log]

Includes:
--

The @Logger annotation:

Instead of writing this...
```
public class LogTest {
    private static final Log log = LogFactory.getLog("mypackage.LogTest");
    public void foo() {
        log.info("Hello Morbok!");
    }
}
```
Just write this:
```
@Logger
public class LogTest {
    public void foo() {
        log.info("Hello Morbok!");
    }
}
```

* See <http://projectlombok.org> for more info about lombok

### Morbok features:

The vanilla @Logger annotation generates an apache commons logger with the variable name "log" and a log name reflecting the fully qualified name of the class it's placed on. 

You can specify SLF4J or JAVA logging if you don't want to generate the default apache commons logger:
```
@Logger(type=Logger.Type.SLF4J) 
@Logger(type=Logger.Type.JAVA)
```

You can also override generating a logger based on the fully qualified class name: 
```
@Logger(value="SPECIAL_LOGGER")
```

And if you don't like your loggers to use a variable named "log" you can override that too: 
```
@Logger(var="myLogger")
```

### Installation Instructions

#### Getting started:

* First start by installing lombok.
* Morbok needs lombok in order to work.  Morbok is compatible with lombok 0.9.x+.
* Go to <http://projectlombok.org> for details.

#### Using javac:

* In order to get morbok working via javac just put it on the compile classpath of your project along with lombok.

#### Using Eclipse:

* Morbok ships with it's own installer... just run the jar.
* If for some reason the installer messes up you can do some manual tweaking.  Do this:
* Find where the lombok installer put the lombok jar.
    1. Windows example: C:\eclipse
    2.  Mac example: /Applications/eclipse/Eclipse.app/Contents/MacOS
* Copy morbok.jar to that same location alongside lombok.jar.
* Find the eclipse.ini file in that folder
* Add morbok.jar to the bootclasspath.  You'll modify it to look like this:
    1. Windows:-Xbootclasspath/a;lombok.jar;morbok.jar
    2. Mac/Linux: -Xbootclasspath/a:lombok.jar:morbok.jar
* Now restart eclipse and you're ready to go.
