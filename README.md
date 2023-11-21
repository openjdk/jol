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

    $ java -jar jol-cli.jar heapdump-stats java_pid92614.hprof
    Heap Dump: java_pid92614.hprof
    Read progress: DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    === Class Histogram

    Table is sorted by "SUM SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

           INSTANCES            SIZE        SUM SIZE    CLASS
    ------------------------------------------------------------------------------------------------
              17.092              24         410.208    java.lang.String
               2.189              80         175.120    long[8]
               4.690              32         150.080    java.util.HashMap$Node
               4.662              32         149.184    com.sun.tools.javac.util.SharedNameTable$NameImpl
                   1         131.088         131.088    com.sun.tools.javac.util.SharedNameTable$NameImpl[32768]
                   1         131.088         131.088    byte[131072]
               3.457              32         110.624    java.util.concurrent.ConcurrentHashMap$Node
               2.189              32          70.048    jdk.internal.jimage.ImageReader$Resource
               2.189              24          52.536    jdk.internal.jimage.ImageLocation
                   4           8.208          32.832    byte[8192]
                   2          16.400          32.800    char[8192]
                 828              32          26.496    java.lang.invoke.MethodType$ConcurrentWeakInternSet$WeakEntry
                 659              40          26.360    java.lang.invoke.MemberName
                 651              40          26.040    java.lang.invoke.MethodType
                 319              80          25.520    java.util.HashMap$Node[16]
                   3           8.208          24.624    java.util.concurrent.ConcurrentHashMap$Node[2048]
               1.515              16          24.240    java.lang.Object
               1.262              16          20.192    java.lang.Integer
                 420              48          20.160    byte[32]
                 335              56          18.760    byte[34]
                 732              24          17.568    java.lang.module.ModuleDescriptor$Exports
                 413              40          16.520    byte[19]
                   1          16.400          16.400    java.util.HashMap$Node[4096]
                 338              48          16.224    java.util.HashMap
                 285              56          15.960    byte[33]
                 270              56          15.120    byte[39]
                 591              24          14.184    byte[3]
                 215              64          13.760    byte[43]
                 156              88          13.728    java.lang.reflect.Method
                 243              56          13.608    byte[35]
                 ...             ...             ...    ...
              29.038         181.048       1.403.288    <other>
    ------------------------------------------------------------------------------------------------
              74.750         493.480       3.214.360    <total>

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

    $ java -jar jol-cli.jar heapdump-duplicates java_pid92614.hprof
    Heap Dump: java_pid92614.hprof
    Read progress: DONE

    Hotspot Layout Simulation (JDK 17, Current VM: 12-byte object headers, 4-byte references, 8-byte aligned objects, 8-byte aligned array bases)

    Heap dump contains 74.750 objects, 3.214.360 bytes in total.

    === Potential Duplication Candidates

    Table is sorted by "SUM SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

                DUPS        SUM SIZE    CLASS
    ------------------------------------------------------------------------------------------------
               1.657         107.144    byte[]
               1.489          49.176    Object[]
               1.514          24.224    java.lang.Object
                   2          16.448    char[]
               1.004          16.064    java.lang.Integer
                 409          13.088    java.util.HashMap$Node
                 580          10.224    int[]
                 516           8.256    java.lang.invoke.ResolvedMethodName
                 234           7.488    java.lang.module.ModuleDescriptor$Requires
                 226           5.424    java.lang.module.ModuleDescriptor$Exports
                 155           3.720    java.lang.String
                 199           3.184    java.lang.invoke.MethodHandleNatives$CallSiteContext
                 127           3.048    java.util.ImmutableCollections$Set12
                 115           2.760    java.util.ArrayList
                  63           2.016    jdk.internal.jimage.ImageReader$Resource
                  70           1.680    jdk.internal.jimage.ImageLocation
                  64           1.536    jdk.internal.module.ServicesCatalog$ServiceProvider
                  51           1.224    java.util.ImmutableCollections$List12
                  36           1.152    java.util.concurrent.ConcurrentHashMap$Node
                  16           1.024    java.util.concurrent.ConcurrentHashMap
                  55             880    com.sun.tools.javac.util.Context$Key
                  27             864    com.sun.tools.javac.util.SharedNameTable$NameImpl
                   4             832    long[]
                  10             400    java.security.AccessControlContext
                   6             240    java.lang.invoke.MemberName
                   3             240    short[]
                  13             208    java.lang.ref.ReferenceQueue$Lock
                   6             192    java.util.ResourceBundle$KeyElementReference
                   4             192    java.util.HashMap
                   5             160    java.lang.invoke.LambdaForm$Name
                 ...             ...    ...
                  38           1.008    <other>
    ------------------------------------------------------------------------------------------------
               8.698         284.096    <total>

    === byte[] Potential Duplicates
      DUPS: Number of instances with same data
      SIZE: Total size taken by duplicate instances

    Table is sorted by "SIZE".
    Printing first 30 lines. Use -DprintFirst=# to override.

                DUPS            SIZE    VALUE
    ------------------------------------------------------------------------------------------------
                   2          16.416    byte[8192] { 0, ..., 0 }
                  10             320    byte[13] (hash: f6ca3f5a7125e6c0)
                   3             312    byte[82] (hash: 7d82d9f161dd916f)
                   7             224    byte[13] (hash: f6ca3f5b04a5c512)
                   1             208    byte[190] (hash: 71b141781c48b4c0)
                   1             168    byte[148] (hash: d951002a3b1499bb)
                   1             160    byte[140] (hash: 8236c4418b62023d)
                   1             152    byte[136] (hash: a354c5ca3e461646)
                   1             144    byte[124] (hash: ed563ca054ab3627)
                   1             144    byte[122] (hash: 5011087831e30920)
                   1             136    byte[120] (hash: 1f3c75b7ebcceac6)
                   2             128    byte[43] (hash: e07d31ada1c0122)
                   4             128    byte[16] (hash: e5c5a7766de82562)
                   2             128    byte[43] (hash: f8e8a02b5ad3decd)
                   2             128    byte[48] (hash: 905ece899b79bfd)

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
                   0            -512    Integer[-128; 256)
                   0          -1.536    Integer[-128; 512)
                 999          12.400    Integer[-128; 1024)
               1.004           8.384    Integer[-128; 2048)
               1.004             192    Integer[-128; 4096)
               1.004         -16.192    Integer[-128; 8192)
               1.004         -48.960    Integer[-128; 16384)
               1.004        -114.496    Integer[-128; 32768)
               1.004        -245.568    Integer[-128; 65536)
               1.004        -507.712    Integer[-128; 131072)
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
