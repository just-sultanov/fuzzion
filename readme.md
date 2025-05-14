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
      (throw (f/issue :high "You found a bug")))
    (catch ArithmeticException _)))
```

Let's add an alias for `clojure.tools.deps`, in which we will indicate where our fuzzers are located.

```clojure
;; deps.edn
{:aliases
  {:fuzz {:extra-paths ["src/fuzz/clojure" "target/classes"]
          :extra-deps {io.github.just-sultanov/fuzzion "RELEASE"}
          :main-opts ["--main" "fuzzion.main"]}}}
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

(defn fuzz:compile
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

$ clojure -T:build fuzz:compile
Copying sources...
Compiling...
```

Our target under the hood has been compiled into a special class `example.core_fuzzer.Square` with one `static` method with the suffix `fuzzerTestOneInput`, which will be called by `libFuzzer`.

Now we have everything ready for fuzzing. Let's run the following commands.

```bash
$ clojure -M:fuzz --timeout 10s

Found 1 target(s) in 1 namespace(s):
  - example.core-fuzzer/square

Configuration:
  - Timeout 10s


Target: example.core-fuzzer/square

[example.core-fuzzer/square] - Command: jazzer --cp=src/fuzz/clojure:target/classes:src/main/clojure:/home/developer/fuzzion/src/main/clojure:/home/developer/fuzzion/src/main/resources:/home/developer/.m2/repository/org/clojure/clojure/1.12.0/clojure-1.12.0.jar:/home/developer/.m2/repository/babashka/fs/0.5.25/fs-0.5.25.jar:/home/developer/.m2/repository/babashka/process/0.6.23/process-0.6.23.jar:/home/developer/.m2/repository/camel-snake-kebab/camel-snake-kebab/0.4.3/camel-snake-kebab-0.4.3.jar:/home/developer/.m2/repository/com/code-intelligence/jazzer-api/0.24.0/jazzer-api-0.24.0.jar:/home/developer/.m2/repository/io/github/tonsky/clj-reload/0.9.6/clj-reload-0.9.6.jar:/home/developer/.m2/repository/org/babashka/cli/0.8.65/cli-0.8.65.jar:/home/developer/.m2/repository/org/clojure/core.specs.alpha/0.4.74/core.specs.alpha-0.4.74.jar:/home/developer/.m2/repository/org/clojure/spec.alpha/0.5.238/spec.alpha-0.5.238.jar --target_class=example.core_fuzzer.Square
[example.core-fuzzer/square] - INFO: Loaded 317 hooks from com.code_intelligence.jazzer.runtime.TraceCmpHooks
[example.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.runtime.TraceDivHooks
[example.core-fuzzer/square] - INFO: Loaded 2 hooks from com.code_intelligence.jazzer.runtime.TraceIndirHooks
[example.core-fuzzer/square] - INFO: Loaded 4 hooks from com.code_intelligence.jazzer.runtime.NativeLibHooks
[example.core-fuzzer/square] - INFO: Loaded 3454 hooks from com.code_intelligence.jazzer.sanitizers.ClojureLangHooks
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
[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer.Square (took 80 ms, size +53%)
[example.core-fuzzer/square] - INFO: found LLVMFuzzerCustomMutator (0x1076a1250). Disabling -len_control by default.
[example.core-fuzzer/square] - INFO: libFuzzer ignores flags that start with '--'
[example.core-fuzzer/square] - INFO: Running with entropic power schedule (0xFF, 100).
[example.core-fuzzer/square] - INFO: Seed: 1946811295
[example.core-fuzzer/square] - INFO: Loaded 1 modules   (512 inline 8-bit counters): 512 [0x7fe6e7c36000, 0x7fe6e7c36200),
[example.core-fuzzer/square] - INFO: Loaded 1 PC tables (512 PCs): 512 [0x7fe6e9934c00,0x7fe6e9936c00),
[example.core-fuzzer/square] - INFO: -max_len is not provided; libFuzzer will not generate inputs larger than 4096 bytes
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.Var (took 209 ms, size +32%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IFn (took 4 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IRef (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IDeref (took 10 ms, size +11%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.Settable (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.ARef (took 21 ms, size +24%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.AReference (took 10 ms, size +10%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IReference (took 0 ms, size +0%)
[example.core-fuzzer/square] - INFO: Instrumented clojure.lang.IMeta (took 0 ms, size +0%)

# ...

[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer$fn__584 (took 4 ms, size +51%)
[example.core-fuzzer/square] - INFO: Instrumented example.core_fuzzer$_square_fuzzerTestOneInput (took 4 ms, size +74%)
[example.core-fuzzer/square] - INFO: A corpus is not provided, starting from an empty corpus
[example.core-fuzzer/square] - #2       pulse  ft: 42 exec/s: 0 rss: 1024Mb
[example.core-fuzzer/square] 00:00:10.000 - #2  INITED cov: 42 ft: 42 corp: 1/1b exec/s: 0 rss: 1024Mb
[example.core-fuzzer/square] 00:00:09.995 - #4  pulse  cov: 42 ft: 42 corp: 1/1b lim: 4 exec/s: 0 rss: 1024Mb
[example.core-fuzzer/square] 00:00:09.995 - #8  pulse  cov: 42 ft: 42 corp: 1/1b lim: 4 exec/s: 0 rss: 1024Mb
[example.core-fuzzer/square] 00:00:09.995 - #16 pulse  cov: 42 ft: 42 corp: 1/1b lim: 4 exec/s: 0 rss: 1024Mb
[example.core-fuzzer/square] 00:00:09.995 - #32 pulse  cov: 42 ft: 42 corp: 1/1b lim: 4 exec/s: 1 rss: 1024Mb
[example.core-fuzzer/square] 00:00:09.986 - INFO: Instrumented clojure.lang.Compiler$FISupport (took 7 ms, size +28%)
[example.core-fuzzer/square] 00:00:09.974 - INFO: Instrumented clojure.asm.Handle (took 11 ms, size +54%)
[example.core-fuzzer/square] 00:00:09.969 -
[example.core-fuzzer/square] 00:00:09.968 - == Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh: You found a bug
[example.core-fuzzer/square] 00:00:09.968 -     at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62)
[example.core-fuzzer/square] 00:00:09.968 -     at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502)
[example.core-fuzzer/square] 00:00:09.968 -     at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486)
[example.core-fuzzer/square] 00:00:09.968 -     at clojure.lang.Reflector.invokeConstructor(Reflector.java:334)
[example.core-fuzzer/square] 00:00:09.968 -     at fuzzion.core$issue.invokeStatic(core.clj:449)
[example.core-fuzzer/square] 00:00:09.968 -     at fuzzion.core$issue.invoke(core.clj:423)
[example.core-fuzzer/square] 00:00:09.968 -     at example.core_fuzzer$_square_fuzzerTestOneInput.invokeStatic(core_fuzzer.clj:18)
[example.core-fuzzer/square] 00:00:09.968 -     at example.core_fuzzer$_square_fuzzerTestOneInput.invoke(core_fuzzer.clj:14)
[example.core-fuzzer/square] 00:00:09.968 -     at example.core_fuzzer.Square.fuzzerTestOneInput(Unknown Source)
[example.core-fuzzer/square] 00:00:09.968 - DEDUP_TOKEN: b44b4a37f6e209bb
[example.core-fuzzer/square] 00:00:09.967 - == libFuzzer crashing input ==
[example.core-fuzzer/square] 00:00:09.967 - MS: 2 ChangeBit-Custom-; base unit: adc83b19e793491b1c6ea0fd8b46cd9f32e592fc
[example.core-fuzzer/square] 00:00:09.967 - 0x2,
[example.core-fuzzer/square] 00:00:09.967 - \002
[example.core-fuzzer/square] 00:00:09.967 - artifact_prefix='./'; Test unit written to ./crash-c4ea21bb365bbeeaf5f2c654883e56d11e43c44e
[example.core-fuzzer/square] 00:00:09.967 - Base64: Ag==
[example.core-fuzzer/square] 00:00:09.960 - reproducer_path='.'; Java reproducer written to ./Crash_c4ea21bb365bbeeaf5f2c654883e56d11e43c44e.java
[example.core-fuzzer/square] 00:00:09.960 -
[example.core-fuzzer/square] Started at: 2025-05-15T00:09:10.326054
[example.core-fuzzer/square] Finished at: 2025-05-15T00:09:34.213513
[example.core-fuzzer/square] Lead time: 00:00:23.887
[example.core-fuzzer/square] Exit code: 1


Fuzzing has been completed for 1 target(s) in 1 namespace(s):
  - example.core-fuzzer/square (00:00:23.887)

Started at: 2025-05-15T00:09:10.325233
Finished at: 2025-05-15T00:09:34.213890
Total lead time: 00:00:23.888
```
