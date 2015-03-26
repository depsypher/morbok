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

