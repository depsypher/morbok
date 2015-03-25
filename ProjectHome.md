## Morbok: Extensions for [Lombok](http://projectlombok.org). ##

### NOTE ###
Lombok now has @Log built in! Morbok is effectively obsoleted now, so just use Lombok instead! See this for more:
[Lombok @Log](http://projectlombok.org/features/Log.html)

### Includes: ###
**The @Logger annotation:**

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


**See http://projectlombok.org for more info about lombok**

&lt;wiki:gadget url="http://www.ohloh.net/p/420096/widgets/project\_users\_logo.xml" height="43" border="0"/&gt;

&lt;wiki:gadget url="http://morbok.googlecode.com/svn/wiki/adsense.xml" border="0" width="468" height="60" /&gt;