# The Lightweight Java Visualizer (LJV)

[![Actions Status: build](https://github.com/atp-mipt/ljv/workflows/build/badge.svg)](https://github.com/atp-mipt/ljv/actions?query=workflow%3A"build")
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.atp-fivt/ljv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.atp-fivt/ljv)

LJV a is tool for visualizing Java data structures, using [Graphviz](http://graphviz.gitlab.io/).

It was developed by [John Hamer](https://www.gla.ac.uk/schools/computing/staff/?webapp=staffcontact&action=person&id=4cdcebe68a94) in 2004 and released under GNU GPL (see the [original project page](https://www.cs.auckland.ac.nz/~j-hamer/LJV.html)).

This project aims to upgrade this tool to modern Java and make it an open source library in the modern sense of the word.

See [documentation](https://atp-mipt.github.io/ljv/) and [JavaDoc](https://atp-mipt.github.io/ljv/apidocs/).

## How to use
The tool requires Java 11 or later version. Pull in the LJV dependency:
```xml
<dependency>
  <groupId>org.atp-fivt</groupId>
  <artifactId>ljv</artifactId>
  <version>1.03</version>
</dependency>
```
Execute the following (`obj` can be any object that you wish to visualize):
```java
public class Main {
    public static void main(String[] args) {
        browse(new LJV(), Map.of(1, 'a', 2, 'b'));
    }

    public static void browse(LJV ljv, Object obj) {
        try {
            var dot = URLEncoder.encode(ljv.drawGraph(obj), "UTF8")
                        .replaceAll("\\+", "%20");
            Desktop.getDesktop().browse(
                new URI("https://dreampuf.github.io/GraphvizOnline/#" 
                        + dot));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
```

A browser window will appear with a diagram that will look like this:

<img src="mapn.png" alt="Immutable MapN internal structure visualization"/>

