# Java Object Layout (JOL)

JOL (Java Object Layout) is the tiny toolbox to analyze object layout
in JVMs. These tools are using Unsafe, JVMTI, and Serviceability Agent (SA)
heavily to decode the actual object layout, footprint, and references.
This makes JOL much more accurate than other tools relying on heap dumps,
specification assumptions, etc.

## Usage

### JOL Samples

You can have a brief tour of JOL capabilities by looking through the [JOL Samples](https://github.com/openjdk/jol/tree/master/jol-samples/src/main/java/org/openjdk/jol/samples). You can run them from the IDE, or using the JAR file:

    $ java -cp jol-samples/target/jol-samples.jar org.openjdk.jol.samples.JOLSample_01_Basic

### Use as Library Dependency

[Maven Central](https://repo.maven.apache.org/maven2/org/openjdk/jol/jol-core/)
contains the latest releases. You can use them right away with this Maven dependency:

    <dependency>
        <groupId>org.openjdk.jol</groupId>
        <artifactId>jol-core</artifactId>
        <version>put-the-version-here</version>
    </dependency>

JOL module would try to self-attach as Java Agent, if possible. If you are using JOL as the library,
it is recommended to add `Premain-Class` and `Launcher-Agent` attributes to the
[final JAR manifest](https://github.com/openjdk/jol/blob/a549b7410045167238716677dac3de221951da2d/jol-samples/pom.xml#L132-L133).

### Use as Command Line Tool

Build produces the self-contained executable JAR in `jol-cli/target/jol-cli.jar`.
Published Maven artifacts also include the executable JAR that one can download
and start using right away. The JAR is published both at
`jol-cli-$version-full.jar` at [Maven Central](https://repo.maven.apache.org/maven2/org/openjdk/jol/jol-cli/) or [here](https://builds.shipilev.net/jol/).

List the supported commands with `-h`:

    $ java -jar jol-cli.jar -h
    Usage: jol-cli.jar <mode> [optional arguments]*

    Available modes:
             estimates: Simulate the class layout in different VM modes.
             externals: Show the object externals: the objects reachable from a given instance.
             footprint: Estimate the footprint of all objects reachable from a given instance
         heapdumpstats: Consume the heap dump and print the most frequent instances.
             internals: Show the object internals: field layout and default contents, object header
                shapes: Dump the object shapes present in JAR files or heap dumps.
       string-compress: Consume the heap dumps and figures out the savings attainable with compressed strings.

A brief tour of commands follows.

#### "internals"

This dives into Object layout: field layout within the object, header information, field values, alignment/padding losses.

    $ java -jar jol-cli.jar internals java.util.HashMap
    # Running 64-bit HotSpot VM.
    # Using compressed oop with 3-bit shift.
    # Using compressed klass with 3-bit shift.
    # Objects are 8 bytes aligned.
    # Field sizes by type: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]
    # Array element sizes: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]

    Instantiated the sample instance via default constructor.

    java.util.HashMap object internals:
    OFF  SZ                       TYPE DESCRIPTION               VALUE
      0   8                            (object header: mark)     0x0000000000000005 (biasable; age: 0)
      8   4                            (object header: class)    0x00019828
     12   4              java.util.Set AbstractMap.keySet        null
     16   4       java.util.Collection AbstractMap.values        null
     20   4                        int HashMap.size              0
     24   4                        int HashMap.modCount          0
     28   4                        int HashMap.threshold         0
     32   4                      float HashMap.loadFactor        0.75
     36   4   java.util.HashMap.Node[] HashMap.table             null
     40   4              java.util.Set HashMap.entrySet          null
     44   4                            (object alignment gap)
    Instance size: 48 bytes
    Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

#### "externals"

This dives into the Object graphs layout: list objects reachable from the instance,
their addresses, paths through the reachability graph, etc (is more
convenient with API though).

    $ java -jar jol-cli.jar externals java.lang.String
    # Running 64-bit HotSpot VM.
    # Using compressed oop with 3-bit shift.
    # Using compressed klass with 3-bit shift.
    # Objects are 8 bytes aligned.
    # Field sizes by type: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]
    # Array element sizes: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]

    Instantiated the sample instance via default constructor.

    java.lang.String@64cee07d object externals:
         ADDRESS       SIZE TYPE             PATH                           VALUE
       58010a600         16 [C               .value                         []
       58010a610    8923824 (something else) (somewhere else)               (something else)
       58098d0c0         24 java.lang.String                                (object)

    Addresses are stable after 1 tries.

#### "footprint"

This gets the object footprint estimate, similar to the object externals, but tabulated.

    $ java -jar jol-cli/target/jol-cli.jar footprint java.lang.Thread
    # Running 64-bit HotSpot VM.
    # Using compressed oop with 3-bit shift.
    # Using compressed klass with 3-bit shift.
    # Objects are 8 bytes aligned.
    # Field sizes by type: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]
    # Array element sizes: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]

    Instantiated the sample instance via default constructor.

    java.lang.Thread@64cee07d footprint:
    COUNT       AVG       SUM   DESCRIPTION
       13        84      1104   [B
      496        67     33400   [C
      139        23      3272   [Ljava.lang.Class;
       17        89      1520   [Ljava.lang.Object;
     ...
        1        24        24   sun.reflect.generics.tree.ClassSignature
        3        16        48   sun.reflect.generics.tree.ClassTypeSignature
        2        24        48   sun.reflect.generics.tree.FormalTypeParameter
        3        24        72   sun.reflect.generics.tree.SimpleClassTypeSignature
     2849              200416   (total)

## Reporting Bugs

You may find unresolved bugs and feature request in 
[JDK Bug System](https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20CODETOOLS%20AND%20resolution%20%3D%20Unresolved%20AND%20component%20%3D%20tools%20AND%20Subcomponent%20%3D%20jol) 
Please submit the new bug there:
 * Project: `CODETOOLS`
 * Component: `tools`
 * Sub-component: `jol`

If you don't have the access to JDK Bug System, submit the bug report at [Issues](https://github.com/openjdk/jol/issues) here, and wait for maintainers to pick that up.

## Development

JOL project accepts pull requests, like other OpenJDK projects.
If you have never contributed to OpenJDK before, then bots would require you to [sign OCA first](https://openjdk.java.net/contribute/).
Normally, you don't need to post patches anywhere else, or post to mailing lists, etc.
If you do want to have a wider discussion about JOL, please refer to [jol-dev](https://mail.openjdk.java.net/mailman/listinfo/jol-dev).

Compile and run tests:

    $ mvn clean verify

Tests would normally run in many JVM configurations. If you are contributing the code,
please try to run the build on multiple JDK releases, most importantly 8u and 11u.
GitHub workflow "JOL Pre-Integration Tests" should pass with your changes.

## Related projects

* [IntelliJ IDEA JOL Plugin](https://github.com/stokito/IdeaJol) can estimate object size and has an inspection to find heavy classes
