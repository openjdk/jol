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

    Available operations:
                 externals: Show object externals: objects reachable from a given instance
                 footprint: Show the footprint of all objects reachable from a sample instance
            heapdump-boxes: Read a heap dump and look for duplicate primitive boxes
       heapdump-duplicates: Read a heap dump and look for probable duplicates
        heapdump-estimates: Read a heap dump and estimate footprint in different VM modes
            heapdump-stats: Read a heap dump and print simple statistics
          heapdump-strings: Read a heap dump and look for duplicate Strings
                 internals: Show object internals: field layout, default contents, object header
       internals-estimates: Same as 'internals', but simulate class layout in different VM modes

A brief tour of commands follows.

#### "internals"

This dives into Object layout: field layout within the object, header information, field values, alignment/padding losses.

    $ java -jar jol-cli.jar internals java.util.HashMap
    # VM mode: 64 bits
    # Compressed references (oops): 3-bit shift
    # Compressed class pointers: 3-bit shift
    # Object alignment: 8 bytes
    #                       ref, bool, byte, char, shrt,  int,  flt,  lng,  dbl
    # Field sizes:            4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array element sizes:    4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array base offsets:    16,   16,   16,   16,   16,   16,   16,   16,   16

    Instantiated the sample instance via default constructor.

    java.util.HashMap object internals:
    OFF  SZ                       TYPE DESCRIPTION               VALUE
      0   8                            (object header: mark)     0x0000000000000001 (non-biasable; age: 0)
      8   4                            (object header: class)    0x000afde8
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

#### "internals-estimates"

This is like `internals`, but simulate the layout in different VM modes. The tools would group similar layouts together,
and sort by instance size, descending.

    % java -jar jol-cli/target/jol-cli.jar internals-estimates java.lang.Integer
    # VM mode: 64 bits
    # Compressed references (oops): 3-bit shift
    # Compressed class pointers: 3-bit shift
    # Object alignment: 8 bytes
    #                       ref, bool, byte, char, shrt,  int,  flt,  lng,  dbl
    # Field sizes:            4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array element sizes:    4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array base offsets:    16,   16,   16,   16,   16,   16,   16,   16,   16

    ***** Hotspot Layout Simulation (JDK 8, 64-bit model, NO compressed references, NO compressed classes, 8-byte aligned)
    ***** Hotspot Layout Simulation (JDK 15, 64-bit model, NO compressed references, NO compressed classes, 8-byte aligned)

    java.lang.Integer object internals:
    OFF  SZ   TYPE DESCRIPTION               VALUE
      0   8        (object header: mark)     N/A
      8   8        (object header: class)    N/A
     16   4    int Integer.value             N/A
     20   4        (object alignment gap)
    Instance size: 24 bytes
    Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

    ***** Hotspot Layout Simulation (JDK 8, 64-bit model, compressed references, compressed classes, 8-byte aligned)
    ***** Hotspot Layout Simulation (JDK 8, 64-bit model, compressed references, compressed classes, 16-byte aligned)
    ***** Hotspot Layout Simulation (JDK 15, 64-bit model, compressed references, compressed classes, 8-byte aligned)
    ***** Hotspot Layout Simulation (JDK 15, 64-bit model, compressed references, compressed classes, 16-byte aligned)
    ***** Hotspot Layout Simulation (JDK 15, 64-bit model, NO compressed references, compressed classes, 8-byte aligned)
    ***** Hotspot Layout Simulation (JDK 15, 64-bit model, NO compressed references, compressed classes, 16-byte aligned)

    java.lang.Integer object internals:
    OFF  SZ   TYPE DESCRIPTION               VALUE
      0   8        (object header: mark)     N/A
      8   4        (object header: class)    N/A
     12   4    int Integer.value             N/A
    Instance size: 16 bytes
    Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

    ***** Hotspot Layout Simulation (JDK 99, 64-bit model, Lilliput (ultimate target), NO compressed references, compressed classes, 8-byte aligned)
    ***** Hotspot Layout Simulation (JDK 99, 64-bit model, Lilliput (ultimate target), compressed references, compressed classes, 8-byte aligned)

    java.lang.Integer object internals:
    OFF  SZ   TYPE DESCRIPTION               VALUE
      0   1        (object header: mark)     N/A
      1   3        (object header: class)    N/A
      4   4    int Integer.value             N/A
    Instance size: 8 bytes
    Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

#### "externals"

This dives into the Object graphs layout: list objects reachable from the instance,
their addresses, paths through the reachability graph, etc (is more
convenient with API though).

    $ java -jar jol-cli.jar externals java.lang.String
    # VM mode: 64 bits
    # Compressed references (oops): 3-bit shift
    # Compressed class pointers: 3-bit shift
    # Object alignment: 8 bytes
    #                       ref, bool, byte, char, shrt,  int,  flt,  lng,  dbl
    # Field sizes:            4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array element sizes:    4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array base offsets:    16,   16,   16,   16,   16,   16,   16,   16,   16

    Instantiated the sample instance via default constructor.

    java.lang.String@61f8bee4d object externals:
              ADDRESS       SIZE TYPE             PATH                           VALUE
            61fa01280         24 java.lang.String                                (object)
            61fa01298 8055156072 (something else) (somewhere else)               (something else)
            7ffc00000         16 byte[]           .value                         []

    Addresses are stable after 1 tries.

#### "footprint"

This gets the object footprint estimate, similar to the object externals, but tabulated.

    $ java -jar jol-cli.jar footprint java.security.SecureRandom
    # VM mode: 64 bits
    # Compressed references (oops): 3-bit shift
    # Compressed class pointers: 3-bit shift
    # Object alignment: 8 bytes
    #                       ref, bool, byte, char, shrt,  int,  flt,  lng,  dbl
    # Field sizes:            4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array element sizes:    4,    1,    1,    2,    2,    4,    4,    8,    8
    # Array base offsets:    16,   16,   16,   16,   16,   16,   16,   16,   16

    Instantiated the sample instance via default constructor.

    java.security.SecureRandom@7fac631bd footprint:

    Table is sorted by "SUM".
    Printing first 30 lines. Use -DprintFirst=# to override.

               COUNT             AVG             SUM    DESCRIPTION
    ------------------------------------------------------------------------------------------------
                 488              46          22.504    byte[]
                 488              24          11.712    java.lang.String
                 318              32          10.176    java.util.concurrent.ConcurrentHashMap.Node
                  53              64           3.392    java.security.Provider.Service
                  41              80           3.280    java.util.HashMap.Node[]
                   2           1.552           3.104    java.util.concurrent.ConcurrentHashMap.Node[]
                 124              24           2.976    java.security.Provider.ServiceKey
                  66              32           2.112    java.util.HashMap.Node
                  40              48           1.920    java.util.HashMap
                  66              24           1.584    java.security.Provider.UString
                  26              28             752    java.lang.Object[]
                  40              16             640    java.util.HashMap.EntrySet
                  26              24             624    java.util.ArrayList
                  14              32             448    java.security.Provider.EngineDescription
                   5              72             360    java.lang.reflect.Field
                   4              72             288    java.lang.reflect.Constructor
                   2             128             256    java.lang.Class
                   5              40             200    java.util.LinkedHashMap.Entry
                   1             144             144    java.lang.ClassValue.Entry[]
                   2              64             128    java.lang.Class.ReflectionData
                   2              64             128    java.util.concurrent.ConcurrentHashMap
                   1             104             104    sun.security.provider.Sun
                   1              80              80    java.util.WeakHashMap.Entry[]
                   2              40              80    java.lang.ref.SoftReference
                   1              64              64    java.lang.ClassValue.ClassValueMap
                   2              32              64    java.lang.ClassValue.Entry
                   1              64              64    java.security.SecureRandom
                   2              28              56    java.lang.reflect.Field[]
                   1              56              56    java.lang.Module
                   1              56              56    java.util.LinkedHashMap
                 ...             ...             ...    ...
                  16             320             360    <other>
    ------------------------------------------------------------------------------------------------
               1.841           3.454          67.712    <total>

#### "heapdump-stats"

Read the heap dump and look into high-level stats for it. The tool runs on heap dump in single pass,
and takes only a little additional memory. This allows processing huge heap dumps on small machines.

    $ java -jar jol-cli.jar heapdump-stats sample-clion.hprof.gz
    Heap Dump: sample-clion.hprof.gz
    Read progress: 269M... 538M... 808M... 1077M... 1346M... 1616M... DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    === Class Histogram

    Table is sorted by "SUM SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

           INSTANCES            SIZE        SUM SIZE    CLASS
    ------------------------------------------------------------------------------------------------
               2.091          65.560     137.085.960    java.lang.Object[16385]
           1.533.042              40      61.321.680    java.util.WeakHashMap$Entry
           1.595.624              24      38.294.976    java.lang.String
               4.627           8.216      38.015.432    java.lang.Object[2049]
           1.074.313              32      34.378.016    java.util.HashMap$Node
             758.446              40      30.337.840    com.jetbrains.cidr.lang.symbols.cpp.OCMacroSymbol
             816.693              32      26.134.176    java.util.concurrent.ConcurrentHashMap$Node
             591.407              40      23.656.280    com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap$WeakKey
             828.635              24      19.887.240    java.util.ArrayList
             612.098              32      19.587.136    com.jetbrains.cidr.lang.types.OCReferenceTypeSimple
             598.784              32      19.161.088    com.jetbrains.cidr.lang.symbols.OCSymbolReference$GlobalReference
             591.328              32      18.922.496    com.intellij.util.containers.ConcurrentWeakKeyWeakValueHashMap$WeakValue
             203.545              80      16.283.600    com.jetbrains.cidr.lang.symbols.cpp.OCDeclaratorSymbolImpl
             223.655              72      16.103.160    com.jetbrains.cidr.lang.symbols.cpp.OCFunctionSymbol
                   1      12.217.040      12.217.040    byte[12217024]
             234.724              40       9.388.960    com.jetbrains.cidr.lang.types.OCFunctionType
               1.132           8.208       9.291.456    java.util.WeakHashMap$Entry[2048]
             229.218              40       9.168.720    com.jetbrains.cidr.lang.types.OCPointerType
              53.663             152       8.156.776    java.lang.Object[33]
             143.195              56       8.018.920    java.lang.Object[10]
               1.612           4.120       6.641.440    java.lang.Object[1025]
             249.059              24       5.977.416    com.jetbrains.cidr.lang.symbols.OCQualifiedName
                 160          32.784       5.245.440    byte[32768]
                 817           6.160       5.032.720    int[1536]
             194.279              24       4.662.696    java.lang.Object[1]
               1.081           4.112       4.445.072    java.util.HashMap$Node[1024]
                   2       2.097.168       4.194.336    java.util.concurrent.ConcurrentHashMap$Node[524288]
                   1       4.194.320       4.194.320    byte[4194304]
              65.486              56       3.667.216    com.intellij.psi.impl.source.tree.CompositeElement
              33.534             104       3.487.536    com.jetbrains.cidr.lang.symbols.cpp.OCStructSymbol
                 ...             ...             ...    ...
           6.783.781      42.772.496     339.519.840    <other>
    ------------------------------------------------------------------------------------------------
          17.426.033      61.411.160     942.478.984    <total>


#### "heapdump-estimates"

Read the heap dump and project the footprint in different VM modes. The tool runs on heap dump in single pass,
and takes only a little additional memory. This allows processing huge heap dumps on small machines. It is useful
to explore if compressed references would pay off with larger alignments, if experimental features like Lilliput
expect to bring a benefit to workload, or if the upgrade to newer JDK or downgrade lower bitness JDK would make sense.

    $ java -jar jol-cli.jar heapdump-estimates sample-clion.hprof.gz
    Heap Dump: sample-clion.hprof.gz

    'Overhead' comes from additional metadata, representation and alignment losses.
    'JVM mode' is the relative footprint change compared to the best JVM mode in this JDK.
    'Upgrade From' is the relative footprint change against the same mode in other JDKs.

    Read progress: 269M... 538M... 808M... 1077M... 1346M... 1616M... DONE

    === Overall Statistics

       17426K,     Total objects
         682M,     Total data size
        39,15,     Average data per object

    === Stock 32-bit OpenJDK

    Footprint,   Overhead,     Description
         897M,     +31,6%,     32-bit (<4 GB heap)

    === Stock 64-bit OpenJDK (JDK < 15)

    Footprint,   Overhead,   JVM Mode,     Description
        1526M,    +123,8%,     +61,9%,     64-bit, no comp refs (>32 GB heap, default align)
         942M,     +38,2%,         0%,     64-bit, comp refs (<32 GB heap, default align)
        1026M,     +50,5%,      +8,9%,     64-bit, comp refs with large align (   32..64GB heap,  16-byte align)
        1133M,     +66,2%,     +20,2%,     64-bit, comp refs with large align (  64..128GB heap,  32-byte align)
        1499M,    +119,8%,     +59,0%,     64-bit, comp refs with large align ( 128..256GB heap,  64-byte align)
        2556M,    +274,7%,    +171,1%,     64-bit, comp refs with large align ( 256..512GB heap, 128-byte align)
        4768M,    +599,1%,    +405,7%,     64-bit, comp refs with large align (512..1024GB heap, 256-byte align)

    === Stock 64-bit OpenJDK (JDK >= 15)

                                         Upgrade From:
    Footprint,   Overhead,   JVM Mode,   JDK < 15,     Description
        1423M,    +108,6%,     +51,0%,      -6,8%,     64-bit, no comp refs, but comp classes (>32 GB heap, default align)
         942M,     +38,2%,         0%,        ~0%,     64-bit, comp refs (<32 GB heap, default align)
        1026M,     +50,4%,      +8,9%,        ~0%,     64-bit, comp refs with large align (   32..64GB heap,  16-byte align)
        1132M,     +66,0%,     +20,1%,      -0,1%,     64-bit, comp refs with large align (  64..128GB heap,  32-byte align)
        1498M,    +119,6%,     +59,0%,        ~0%,     64-bit, comp refs with large align ( 128..256GB heap,  64-byte align)
        2556M,    +274,7%,    +171,2%,        ~0%,     64-bit, comp refs with large align ( 256..512GB heap, 128-byte align)
        4768M,    +599,1%,    +406,0%,         0%,     64-bit, comp refs with large align (512..1024GB heap, 256-byte align)

    === Experimental 64-bit OpenJDK: Lilliput, 64-bit headers

                                         Upgrade From:
    Footprint,   Overhead,   JVM Mode,   JDK < 15,  JDK >= 15,     Description
        1373M,    +101,3%,     +51,9%,     -10,0%,      -3,5%,     64-bit, no comp refs, but comp classes (>32 GB heap, default align)
         904M,     +32,6%,         0%,      -4,1%,      -4,0%,     64-bit, comp refs (<32 GB heap, default align)
        1001M,     +46,8%,     +10,7%,      -2,5%,      -2,4%,     64-bit, comp refs with large align (   32..64GB heap,  16-byte align)
        1116M,     +63,6%,     +23,4%,      -1,5%,      -1,4%,     64-bit, comp refs with large align (  64..128GB heap,  32-byte align)
        1496M,    +119,3%,     +65,4%,      -0,2%,      -0,1%,     64-bit, comp refs with large align ( 128..256GB heap,  64-byte align)
        2556M,    +274,7%,    +182,6%,        ~0%,        ~0%,     64-bit, comp refs with large align ( 256..512GB heap, 128-byte align)
        4768M,    +599,1%,    +427,3%,         0%,         0%,     64-bit, comp refs with large align (512..1024GB heap, 256-byte align)

    === Experimental 64-bit OpenJDK: Lilliput, 32-bit headers

                                         Upgrade From:
    Footprint,   Overhead,   JVM Mode,   JDK < 15,  JDK >= 15,    Lill-64,      Description
        1283M,     +88,2%,     +59,8%,     -15,9%,      -9,8%,      -6,5%,      64-bit, no comp refs, but comp classes (>32 GB heap, default align)
         803M,     +17,7%,         0%,     -14,8%,     -14,8%,     -11,2%,      64-bit, comp refs (<32 GB heap, default align)
         858M,     +25,9%,      +6,9%,     -16,4%,     -16,3%,     -14,2%,      64-bit, comp refs with large align (   32..64GB heap,  16-byte align)
         972M,     +42,5%,     +21,0%,     -14,2%,     -14,1%,     -12,9%,      64-bit, comp refs with large align (  64..128GB heap,  32-byte align)
        1477M,    +116,5%,     +83,9%,      -1,5%,      -1,4%,      -1,3%,      64-bit, comp refs with large align ( 128..256GB heap,  64-byte align)
        2554M,    +274,5%,    +218,1%,     -46,4%,        ~0%,        ~0%,      64-bit, comp refs with large align ( 256..512GB heap, 128-byte align)
        4768M,    +599,0%,    +493,8%,        ~0%,        ~0%,        ~0%,      64-bit, comp refs with large align (512..1024GB heap, 256-byte align)


#### "heapdump-duplicates"

Reads the heap dump and tries to identify the objects that have the same contents. These objects might be de-duplicated,
if possible. It would print both the summary report, and more verbose report per class. The tool runs on heap dump in single pass,
and takes some memory to store hashes for duplicate objects. This allows processing huge heap dumps without having lots
of memory. Bump the heap size for the tool if heap dump does not fit.

    $ java -jar jol-cli.jar heapdump-duplicates sample-clion.hprof.gz
    Heap Dump: sample-clion.hprof.gz
    Read progress: 269M... 538M... 808M... 1077M... 1346M... 1616M... DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    Heap dump contains 17.426.033 objects, 942.478.984 bytes in total.

    === Potential Duplication Candidates

    Table is sorted by "SUM SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

                DUPS        SUM SIZE    CLASS
    ------------------------------------------------------------------------------------------------
             449.993     124.302.944    Object[]
             656.318      31.629.792    byte[]
             661.645      26.465.800    com.jetbrains.cidr.lang.symbols.cpp.OCMacroSymbol
             610.393      19.532.576    java.util.HashMap$Node
             177.670       6.566.216    int[]
             273.211       6.557.064    java.util.ArrayList
              93.361       2.987.552    java.util.concurrent.ConcurrentHashMap$Node
              59.492       1.903.744    com.jetbrains.cidr.lang.types.OCReferenceTypeSimple
              39.348       1.573.920    com.jetbrains.cidr.lang.types.OCPointerType
              62.699       1.504.776    java.lang.String
              32.250       1.290.000    org.languagetool.rules.patterns.PatternToken
              50.661       1.215.864    com.intellij.openapi.util.Pair
               7.536       1.033.872    long[]
              25.407       1.016.280    com.jetbrains.cidr.lang.types.OCIntType
              63.268       1.012.288    java.util.concurrent.atomic.AtomicReference
              29.521         944.672    com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
              56.675         906.800    java.lang.Integer
              21.845         699.040    com.jetbrains.cidr.lang.symbols.OCSymbolReference$GlobalReference
              42.183         674.928    java.lang.Object
              27.481         659.544    com.intellij.util.keyFMap.OneElementFMap
              19.553         625.696    com.jetbrains.cidr.lang.symbols.ComplexTextRange
              14.071         562.840    com.intellij.reference.SoftReference
               7.288         524.736    java.lang.reflect.Field
              21.370         512.880    com.jetbrains.cidr.lang.symbols.OCQualifiedName
              12.625         505.000    java.lang.ref.SoftReference
              10.224         490.752    java.util.HashMap
               2.362         481.664    boolean[]
               9.355         449.040    com.jetbrains.cidr.lang.preprocessor.OCMacroForeignLeafType
              17.707         424.968    com.jetbrains.cidr.lang.symbols.cpp.OCIncludeSymbol$IncludePath
              10.533         421.320    com.jetbrains.cidr.lang.preprocessor.OCMacroReferenceTokenType
                 ...             ...    ...
             307.252       9.288.760    <other>
    ------------------------------------------------------------------------------------------------
           3.873.297     246.765.328    <total>

    ...

    === com.jetbrains.cidr.lang.symbols.cpp.OCMacroSymbol Potential Duplicates
      DUPS: Number of instances with same data
      SIZE: Total size taken by duplicate instances

    Table is sorted by "SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

                DUPS            SIZE    VALUE
    ------------------------------------------------------------------------------------------------
               1.044          41.760    (hash: b3d7653a1b45cdc7)
               1.044          41.760    (hash: dba02bbacfe63eb7)
               1.044          41.760    (hash: 31921ef6e494ca97)
               1.044          41.760    (hash: c2b4fb34818eb9ed)
               1.044          41.760    (hash: 31f79d3ace1161ca)
               1.044          41.760    (hash: 13f841d0438614c5)
               1.044          41.760    (hash: d45cdf077af876ad)
               1.044          41.760    (hash: 1b27a7c37cafc70e)
    ...

#### "heapdump-boxes"

Similar to `heapdump-duplicates`, but concentrates on primitive boxes. It gives a bit more detailed idea
what are the ranges of primitive boxes the workload deals with, and what deduplication/caching strategies
might apply. The tool runs on heap dump in single pass, and takes some memory to store values for duplicate
boxes. This allows processing huge heap dumps without having lots of memory. Bump the heap size for the tool
if heap dump does not fit.

    % java -jar jol-cli.jar heapdump-boxes java_pid92614.hprof
    Heap Dump: java_pid92614.hprof
    Read progress: DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    Heap dump contains 74.750 objects, 3.214.360 bytes in total.

    === java.lang.Integer boxes:

    Table is sorted by "VALUE".

                DUPS       SUM BYTES    VALUE
    ------------------------------------------------------------------------------------------------
                 999          15.984    1000
                   5              80    1024
    ------------------------------------------------------------------------------------------------

    === java.lang.Integer, savings with manual cache, or non-default AutoBoxCacheMax:

     SAVED INSTANCES     SAVED BYTES    CACHE SHAPE
    ------------------------------------------------------------------------------------------------
                   0            -512    Integer[256]
                   0          -1.536    Integer[512]
                 999          12.400    Integer[1024]
               1.004           8.384    Integer[2048]
               1.004             192    Integer[4096]
               1.004         -16.192    Integer[8192]
               1.004         -48.960    Integer[16384]
               1.004        -114.496    Integer[32768]
               1.004        -245.568    Integer[65536]
               1.004        -507.712    Integer[131072]
                 ...             ...    ...
    ------------------------------------------------------------------------------------------------

    === java.lang.Integer, savings with manual cache:

     SAVED INSTANCES     SAVED BYTES    CACHE SHAPE
    ------------------------------------------------------------------------------------------------
              16.064          15.952    HashMap<Integer, Integer>(256)
              16.064          15.952    HashMap<Integer, Integer>(512)
              16.064          15.952    HashMap<Integer, Integer>(1024)
              16.064          15.952    HashMap<Integer, Integer>(2048)
              16.064          15.952    HashMap<Integer, Integer>(4096)
              16.064          15.952    HashMap<Integer, Integer>(8192)
              16.064          15.952    HashMap<Integer, Integer>(16384)
              16.064          15.952    HashMap<Integer, Integer>(32768)
              16.064          15.952    HashMap<Integer, Integer>(65536)
              16.064          15.952    HashMap<Integer, Integer>(131072)
              16.064          15.952    HashMap<Integer, Integer>(262144)
                 ...             ...    ...
    ------------------------------------------------------------------------------------------------

#### "heapdump-strings"

Similar to `heapdump-duplicates`, but concentrates on Strings. It gives a bit more detailed idea
how many duplicate Strings are in workload, and what deduplication/caching strategies might apply. The tool runs
on the heap dump in two passes, and takes some memory to store values for duplicate Strings. This allows processing
huge heap dumps without having lots of memory. Bump the heap size for the tool if heap dump does not fit.

    % java -jar jol-cli.jar heapdump-strings sample-clion.hprof.gz
    Heap Dump: sample-clion.hprof.gz

    Discovering String objects...
    Read progress: 269M... 538M... 808M... 1077M... 1346M... 1616M... DONE

    Discovering String contents...
    Read progress: 269M... 538M... 808M... 1077M... 1346M... 1616M... DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    Heap dump contains 17.426.033 objects, 942.478.984 bytes in total.

    === Duplicate Strings
      DUPS: Number of duplicated String instances
      SIZE (V): Savings due to String.value dedup (automatic by GC)
      SIZE (S+V): Savings due to entire String dedup (manual)

    Table is sorted by "SIZE (S+V)".
    Printing first 30 lines. Use -DprintFirst=# to override.

                DUPS        SIZE (V)      SIZE (S+V)    VALUE
    ------------------------------------------------------------------------------------------------
               5.530         575.120         707.840    /Users/shipilev/Work/shipilev-li... (85 chars)
               2.135         307.440         358.680    -I/Users/shipilev/Work/shipilev-... (123 chars)
               2.135         290.360         341.600    -I/Users/shipilev/Work/shipilev-... (115 chars)
               1.937         278.928         325.416    -I/Users/shipilev/Work/shipilev-... (126 chars)
               1.902         273.888         319.536    /Applications/Xcode.app/Contents... (124 chars)
               1.937         263.432         309.920    -I/Users/shipilev/Work/shipilev-... (119 chars)
               2.278         236.912         291.584    /Users/shipilev/Work/shipilev-li... (85 chars)
               1.351         248.584         281.008     <a href="#insp... (162 chars)
               1.903         228.360         274.032    /Applications/Xcode.app/Contents... (98 chars)
               2.135         204.960         256.200    -I/Users/shipilev/Work/shipilev-... (78 chars)
               2.135         187.880         239.120    -I/Users/shipilev/Work/shipilev-... (68 chars)
               2.135         187.880         239.120    -I/Users/shipilev/Work/shipilev-... (65 chars)
               2.135         187.880         239.120    -I/Users/shipilev/Work/shipilev-... (71 chars)
               2.135         187.880         239.120    -I/Users/shipilev/Work/shipilev-... (66 chars)
               1.937         185.952         232.440    -I/Users/shipilev/Work/shipilev-... (76 chars)
               1.937         185.952         232.440    -I/Users/shipilev/Work/shipilev-... (73 chars)
                  47         209.808         210.936    {"Checks":"-*,bugprone-argument-... (4447 chars)
               1.045         175.560         200.640    /Users/shipilev/Work/shipilev-li... (150 chars)
                   4         192.352         192.448    #define __llvm__ 1 define __cla... (48066 chars)
               1.903         106.568         152.240    -DMAC_OS_X_VERSION_MIN_REQUIRED=... (38 chars)
               1.091         113.464         139.648    -I/Users/shipilev/Work/shipilev-... (84 chars)
               1.903          91.344         137.016    -mmacosx-version-min=11.00.00
               1.902          91.296         136.944    -fno-delete-null-pointer-checks
               1.902          91.296         136.944    -Wno-unknown-warning-option
               1.085         104.160         130.200    -I/Users/shipilev/Work/shipilev-... (77 chars)
               1.004         104.416         128.512    -I/Users/shipilev/Work/shipilev-... (81 chars)
               1.003         104.312         128.384    -I/Users/shipilev/Work/shipilev-... (82 chars)
               2.250          72.000         126.000    LOCAL_VARIABLE
               1.902          76.080         121.728    -Woverloaded-virtual
               1.902          76.080         121.728    -Wunused-function
               ...             ...             ...    ...
             597.115      24.285.016      38.615.776    <other>
    ------------------------------------------------------------------------------------------------
             651.715      29.925.160      45.566.320    <total>


## Reporting Bugs

You may find unresolved bugs and feature request in 
[JDK Bug System](https://bugs.openjdk.java.net/issues/?jql=project%20%3D%20CODETOOLS%20AND%20resolution%20%3D%20Unresolved%20AND%20component%20%3D%20tools%20AND%20Subcomponent%20%3D%20jol) 
Please submit the new bug there:
 * Project: `CODETOOLS`
 * Component: `tools`
 * Sub-component: `jol`

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
