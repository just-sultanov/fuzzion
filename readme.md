# fuzzion

A Clojure wrapper library for [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) ([libFuzzer](https://llvm.org/docs/LibFuzzer.html)).
Coverage-guided, in-process fuzzing for the JVM.

## Getting started

For example, you have such a `square` function and you want to fuzz it.

```clojure
;; src/main/clojure/example/core.clj
(ns example.core)

(defn square
  [x]
  (* x x))
```

Let's add a simple target.

```clojure
;; src/fuzz/clojure/example/core_fuzzer.clj
(ns example.core-fuzzer
  (:require
    [example.core :as sut]
    [fuzzion.core :as f]))

(f/deftarget square
  [input]
  (try
    (when (= 4 (sut/square (f/consume-long input)))
      (throw (f/issue :high "You are found a bug")))
    (catch ArithmeticException _)))
```

Let's add an alias for `clojure.tools.deps`, in which we will indicate where our fuzzers are located.

```clojure
;; deps.edn
{:aliases
  {:fuzz {:extra-paths ["src/fuzz/clojure" "target/classes"]}}}
```

Before starting fuzzing, we will need to compile the source code with the targets.
We can add a simple task using `clojure.tools.build`.

```clojure
;; build.clj
(ns build
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as b]))

(defn clean
  [_]
  (println "Cleaning...")
  (b/delete {:path "target"}))

(defn compile-fuzzers
  [_]
  (println "Copying sources...")
  (b/copy-dir {:src-dirs ["src/main/clojure" "src/fuzz/clojure"]
               :target-dir "target/classes"})
  (println "Compiling...")
  (b/compile-clj {:basis (b/create-basis {:project "deps.edn", :aliases [:fuzz]})
                  :ns-compile '[example.core-fuzzer] ;; specify the required namespaces
                  :class-dir "target/classes"}))
```

```bash
$ clojure -T:build clean
Cleaning...

$ clojure -T:build compile-fuzzers
Copying sources...
Compiling...
```

Our target under the hood has been compiled into a special class `example.core_fuzzer.Square` with one `static` method with the suffix `fuzzerTestOneInput`, which will be called by `libFuzzer`.

Now we have everything ready for fuzzing. Let's run the following commands.

```bash
$ clojure -M:fuzz --main fuzzion.main --timeout 10s
--------------------------------------------------------------------------------
Found 1 target(s) in 1 namespace(s)
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
Target: example.core-fuzzer/square
Timeout: 10s
--------------------------------------------------------------------------------
Command: jazzer --cp=src/develop/clojure:src/develop/resources:src/fuzz/clojure:target/classes:src/main/clojure:src/main/resources:/home/researcher/.m2/repository/babashka/fs/0.5.24/fs-0.5.24.jar:/home/researcher/.m2/repository/babashka/process/0.6.23/process-0.6.23.jar:/home/researcher/.m2/repository/camel-snake-kebab/camel-snake-kebab/0.4.3/camel-snake-kebab-0.4.3.jar:/home/researcher/.m2/repository/cider/cider-nrepl/0.55.2/cider-nrepl-0.55.2.jar:/home/researcher/.m2/repository/com/code-intelligence/jazzer-api/0.24.0/jazzer-api-0.24.0.jar:/home/researcher/.m2/repository/io/github/tonsky/clj-reload/0.9.4/clj-reload-0.9.4.jar:/home/researcher/.m2/repository/metosin/jsonista/0.3.13/jsonista-0.3.13.jar:/home/researcher/.m2/repository/nrepl/nrepl/1.3.1/nrepl-1.3.1.jar:/home/researcher/.m2/repository/org/babashka/cli/0.8.65/cli-0.8.65.jar:/home/researcher/.m2/repository/org/clojure/clojure/1.12.0/clojure-1.12.0.jar:/home/researcher/.m2/repository/cider/orchard/0.34.0/orchard-0.34.0.jar:/home/researcher/.m2/repository/mx/cider/logjam/0.3.0/logjam-0.3.0.jar:/home/researcher/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar:/home/researcher/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar:/home/researcher/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.18.2/jackson-datatype-jsr310-2.18.2.jar:/home/researcher/.m2/repository/org/clojure/core.specs.alpha/0.4.74/core.specs.alpha-0.4.74.jar:/home/researcher/.m2/repository/org/clojure/spec.alpha/0.5.238/spec.alpha-0.5.238.jar:/home/researcher/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar --target_class=example.core_fuzzer.Square
[example.core-fuzzer/square] - INFO: Loaded 320 hooks from com.code_intelligence.jazzer.runtime.TraceCmpHooks
[example.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.runtime.TraceDivHooks
[example.core-fuzzer/square] - INFO: Loaded 2 hooks from com.code_intelligence.jazzer.runtime.TraceIndirHooks
[example.core-fuzzer/square] - INFO: Loaded 4 hooks from com.code_intelligence.jazzer.runtime.NativeLibHooks
[example.core-fuzzer/square] - INFO: Loaded 3370 hooks from com.code_intelligence.jazzer.sanitizers.ClojureLangHooks
[example.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.sanitizers.Deserialization
[example.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.sanitizers.ExpressionLanguageInjection
[example.core-fuzzer/square] - INFO: Loaded 70 hooks from com.code_intelligence.jazzer.sanitizers.LdapInjection
[example.core-fuzzer/square] - INFO: Loaded 46 hooks from com.code_intelligence.jazzer.sanitizers.NamingContextLookup
[example.core-fuzzer/square] - INFO: Loaded 1 hooks from com.code_intelligence.jazzer.sanitizers.OsCommandInjection
[example.core-fuzzer/square] - INFO: Loaded 52 hooks from com.code_intelligence.jazzer.sanitizers.ReflectiveCall
[example.core-fuzzer/square] - INFO: Loaded 8 hooks from com.code_intelligence.jazzer.sanitizers.RegexInjection
[example.core-fuzzer/square] - INFO: Loaded 16 hooks from com.code_intelligence.jazzer.sanitizers.RegexRoadblocks
[example.core-fuzzer/square] - INFO: Loaded 12 hooks from com.code_intelligence.jazzer.sanitizers.ScriptEngineInjection
[example.core-fuzzer/square] - INFO: Loaded 3 hooks from com.code_intelligence.jazzer.sanitizers.ServerSideRequestForgery
[example.core-fuzzer/square] - INFO: Loaded 19 hooks from com.code_intelligence.jazzer.sanitizers.SqlInjection
[example.core-fuzzer/square] - INFO: Loaded 6 hooks from com.code_intelligence.jazzer.sanitizers.XPathInjection
[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer.Square (took 65 ms, size +53%)
[example.core-fuzzer/square] - INFO: found LLVMFuzzerCustomMutator (0x10830d250). Disabling -len_control by default.
[example.core-fuzzer/square] - INFO: libFuzzer ignores flags that start with '--'
[example.core-fuzzer/square] - INFO: Running with entropic power schedule (0xFF, 100).
[example.core-fuzzer/square] - INFO: Seed: 1170429784
[example.core-fuzzer/square] - INFO: Loaded 1 modules   (512 inline 8-bit counters): 512 [0x7fa20e800000, 0x7fa20e800200),
[example.core-fuzzer/square] - INFO: Loaded 1 PC tables (512 PCs): 512 [0x7fa2079ed200,0x7fa2079ef200),
[example.core-fuzzer/square] - INFO: -max_len is not provided; libFuzzer will not generate inputs larger than 4096 bytes
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.Var (took 140 ms, size +32%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IFn (took 3 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IRef (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IDeref (took 10 ms, size +11%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.Settable (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.ARef (took 19 ms, size +24%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.AReference (took 9 ms, size +10%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IReference (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IMeta (took 0 ms, size +0%)

...

[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$fn__335 (took 4 ms, size +53%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$generate_class_name (took 5 ms, size +59%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$deftarget (took 8 ms, size +132%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_boolean (took 4 ms, size +35%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_booleans (took 3 ms, size +18%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_byte (took 6 ms, size +17%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_bytes (took 4 ms, size +18%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_long (took 9 ms, size +16%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IFn$OLLL (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_longs (took 4 ms, size +18%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$consume_remaining_as_string (took 4 ms, size +20%)
[example.core-fuzzer/square] - INFO: Instrumented fuzzion.core$issue (took 9 ms, size +37%)
[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer$fn__355 (took 3 ms, size +51%)
[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer$_square_fuzzerTestOneInput (took 3 ms, size +74%)
[example.core-fuzzer/square] - INFO: A corpus is not provided, starting from an empty corpus
[example.core-fuzzer/square] - #2       pulse  ft: 42 exec/s: 0 rss: 987Mb
[example.core-fuzzer/square] 00:00:10.000 - #2  INITED cov: 42 ft: 42 corp: 1/1b exec/s: 0 rss: 987Mb
[example.core-fuzzer/square] 00:00:09.995 - #4  pulse  cov: 42 ft: 42 corp: 1/1b lim: 4 exec/s: 0 rss: 987Mb
[example.core-fuzzer/square] 00:00:09.993 -
[example.core-fuzzer/square] 00:00:09.993 - == Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh: You are found a bug
[example.core-fuzzer/square] 00:00:09.993 -     at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62)
[example.core-fuzzer/square] 00:00:09.993 -     at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502)
[example.core-fuzzer/square] 00:00:09.993 -     at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486)
[example.core-fuzzer/square] 00:00:09.993 -     at clojure.lang.Reflector.invokeConstructor(Reflector.java:334)
[example.core-fuzzer/square] 00:00:09.993 -     at fuzzion.core$issue.invokeStatic(core.clj:112)
[example.core-fuzzer/square] 00:00:09.993 -     at fuzzion.core$issue.invoke(core.clj:103)
[example.core-fuzzer/square] 00:00:09.993 -     at example.core_fuzzer$_square_fuzzerTestOneInput.invokeStatic(core_fuzzer.clj:18)
[example.core-fuzzer/square] 00:00:09.992 -     at example.core_fuzzer$_square_fuzzerTestOneInput.invoke(core_fuzzer.clj:14)
[example.core-fuzzer/square] 00:00:09.992 -     at example.core_fuzzer.Square.fuzzerTestOneInput(Unknown Source)
[example.core-fuzzer/square] 00:00:09.992 - DEDUP_TOKEN: ae8749e779ed7dd1
[example.core-fuzzer/square] 00:00:09.992 - == libFuzzer crashing input ==
[example.core-fuzzer/square] 00:00:09.992 - MS: 2 ChangeBit-Custom-; base unit: adc83b19e793491b1c6ea0fd8b46cd9f32e592fc
[example.core-fuzzer/square] 00:00:09.992 - 0x2,
[example.core-fuzzer/square] 00:00:09.992 - \002
[example.core-fuzzer/square] 00:00:09.992 - artifact_prefix='./'; Test unit written to ./crash-c4ea21bb365bbeeaf5f2c654883e56d11e43c44e
[example.core-fuzzer/square] 00:00:09.992 - Base64: Ag==
[example.core-fuzzer/square] 00:00:09.985 - reproducer_path='.'; Java reproducer written to ./Crash_c4ea21bb365bbeeaf5f2c654883e56d11e43c44e.java
[example.core-fuzzer/square] 00:00:09.985 -
--------------------------------------------------------------------------------
[example.core-fuzzer/square] Started at: 2025-04-29T23:20:31.180683
[example.core-fuzzer/square] Finished at: 2025-04-29T23:20:48.662337
[example.core-fuzzer/square] Total lead time: 00:00:17.481
[example.core-fuzzer/square] Exit code: 1
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
Started at: 2025-04-29T23:20:31.179589
Finished at: 2025-04-29T23:20:48.662771
Total lead time: 00:00:17.483
--------------------------------------------------------------------------------
```
