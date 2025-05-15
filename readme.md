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

Configuration:
  - Timeout: 10s
  - NS patterns: [".+-fuzzer$"]
  - Skip meta: [:skip]

Found 1 target(s) in 1 namespace(s):
  - example.fuzzers.core-fuzzer/square


Target: example.fuzzers.core-fuzzer/square

[example.fuzzers.core-fuzzer/square] - Command: jazzer --reproducer_path=fuzz/reproducers/example/fuzzers/core_fuzzer/square/ -use_value_profile=1 --coverage_report=fuzz/coverage/example/fuzzers/core_fuzzer/square/report.txt --target_class=example.fuzzers.core_fuzzer.Square --instrumentation_includes=example.** -print_coverage=1 -create_missing_dirs=1 --cp=src/fuzz/clojure:target/classes:src/main/clojure:/home/developer/fuzzion/src/main/clojure:/home/developer/fuzzion/src/main/resources:/home/developer/.m2/repository/org/clojure/clojure/1.12.0/clojure-1.12.0.jar:/home/developer/.m2/repository/babashka/fs/0.5.25/fs-0.5.25.jar:/home/developer/.m2/repository/babashka/process/0.6.23/process-0.6.23.jar:/home/developer/.m2/repository/camel-snake-kebab/camel-snake-kebab/0.4.3/camel-snake-kebab-0.4.3.jar:/home/developer/.m2/repository/com/code-intelligence/jazzer-api/0.24.0/jazzer-api-0.24.0.jar:/home/developer/.m2/repository/io/github/tonsky/clj-reload/0.9.6/clj-reload-0.9.6.jar:/home/developer/.m2/repository/org/babashka/cli/0.8.65/cli-0.8.65.jar:/home/developer/.m2/repository/org/clojure/core.specs.alpha/0.4.74/core.specs.alpha-0.4.74.jar:/home/developer/.m2/repository/org/clojure/spec.alpha/0.5.238/spec.alpha-0.5.238.jar -timeout=300 -print_final_stats=1 --keep_going=10 -reduce_inputs=0 -print_corpus_stats=1 --coverage_dump=fuzz/coverage/example/fuzzers/core_fuzzer/square/dump.exec -dict=fuzz/dicts/example/fuzzers/core_fuzzer/square/dict --instrumentation_excludes=example.fuzzers.** -print_full_coverage=1 -artifact_prefix=fuzz/crashes/example/fuzzers/core_fuzzer/square/ --jvm_args=--enable-preview:-Xmx1000m:-XX:-OmitStackTraceInFastThrow:-XX:+UseParallelGC:-XX:+CriticalJNINative:-XX:+EnableDynamicAgentLoading fuzz/corpus/example/fuzzers/core_fuzzer/square
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 311 hooks from com.code_intelligence.jazzer.runtime.TraceCmpHooks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.runtime.TraceDivHooks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 2 hooks from com.code_intelligence.jazzer.runtime.TraceIndirHooks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 4 hooks from com.code_intelligence.jazzer.runtime.NativeLibHooks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 3388 hooks from com.code_intelligence.jazzer.sanitizers.ClojureLangHooks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.sanitizers.Deserialization
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 5 hooks from com.code_intelligence.jazzer.sanitizers.ExpressionLanguageInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 70 hooks from com.code_intelligence.jazzer.sanitizers.LdapInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 46 hooks from com.code_intelligence.jazzer.sanitizers.NamingContextLookup
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 1 hooks from com.code_intelligence.jazzer.sanitizers.OsCommandInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 52 hooks from com.code_intelligence.jazzer.sanitizers.ReflectiveCall
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 8 hooks from com.code_intelligence.jazzer.sanitizers.RegexInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 16 hooks from com.code_intelligence.jazzer.sanitizers.RegexRoadblocks
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 12 hooks from com.code_intelligence.jazzer.sanitizers.ScriptEngineInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 3 hooks from com.code_intelligence.jazzer.sanitizers.ServerSideRequestForgery
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 19 hooks from com.code_intelligence.jazzer.sanitizers.SqlInjection
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 6 hooks from com.code_intelligence.jazzer.sanitizers.XPathInjection
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented example.fuzzers.core_fuzzer.Square with custom hooks only (took 42 ms, size +19%)
[example.fuzzers.core-fuzzer/square] - INFO: using inputs from: fuzz/corpus/example/fuzzers/core_fuzzer/square
[example.fuzzers.core-fuzzer/square] - INFO: found LLVMFuzzerCustomMutator (0x109c06250). Disabling -len_control by default.
[example.fuzzers.core-fuzzer/square] - INFO: libFuzzer ignores flags that start with '--'
[example.fuzzers.core-fuzzer/square] - INFO: Running with entropic power schedule (0xFF, 100).
[example.fuzzers.core-fuzzer/square] - INFO: Seed: 1499000910
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 1 modules   (512 inline 8-bit counters): 512 [0x7fe74a800000, 0x7fe74a800200),
[example.fuzzers.core-fuzzer/square] - INFO: Loaded 1 PC tables (512 PCs): 512 [0x7fe742ae0200,0x7fe742ae2200),
[example.fuzzers.core-fuzzer/square] - INFO:      105 files found in fuzz/corpus/example/fuzzers/core_fuzzer/square
[example.fuzzers.core-fuzzer/square] - INFO: -max_len is not provided; libFuzzer will not generate inputs larger than 4096 bytes
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.Var with custom hooks only (took 98 ms, size +21%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.IFn with custom hooks only (took 0 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.IRef with custom hooks only (took 0 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.IDeref with custom hooks only (took 5 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.Settable with custom hooks only (took 0 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.ARef with custom hooks only (took 11 ms, size +11%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.AReference with custom hooks only (took 5 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.IReference with custom hooks only (took 0 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.IMeta with custom hooks only (took 0 ms, size +0%)

# ...

[example.fuzzers.core-fuzzer/square] - INFO: Instrumented example.fuzzers.core_fuzzer$fn__364 with custom hooks only (took 5 ms, size +39%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented example.fuzzers.core_fuzzer$_square_fuzzerTestOneInput with custom hooks only (took 5 ms, size +29%)
[example.fuzzers.core-fuzzer/square] - INFO: seed corpus: files: 105 min: 1b max: 210b total: 2707b rss: 742Mb
[example.fuzzers.core-fuzzer/square] - #2       pulse  ft: 4 exec/s: 0 rss: 742Mb
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.lang.Compiler$FISupport with custom hooks only (took 6 ms, size +0%)
[example.fuzzers.core-fuzzer/square] - INFO: Instrumented clojure.asm.Handle with custom hooks only (took 9 ms, size +0%)
[example.fuzzers.core-fuzzer/square] -
[example.fuzzers.core-fuzzer/square] - == Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh: You found a bug
[example.fuzzers.core-fuzzer/square] -  at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62)
[example.fuzzers.core-fuzzer/square] -  at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502)
[example.fuzzers.core-fuzzer/square] -  at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486)
[example.fuzzers.core-fuzzer/square] -  at clojure.lang.Reflector.invokeConstructor(Reflector.java:334)
[example.fuzzers.core-fuzzer/square] -  at fuzzion.core$issue.invokeStatic(core.clj:451)
[example.fuzzers.core-fuzzer/square] -  at fuzzion.core$issue.invoke(core.clj:425)
[example.fuzzers.core-fuzzer/square] -  at example.fuzzers.core_fuzzer$_square_fuzzerTestOneInput.invokeStatic(core_fuzzer.clj:19)
[example.fuzzers.core-fuzzer/square] -  at example.fuzzers.core_fuzzer$_square_fuzzerTestOneInput.invoke(core_fuzzer.clj:15)
[example.fuzzers.core-fuzzer/square] -  at example.fuzzers.core_fuzzer.Square.fuzzerTestOneInput(Unknown Source)
[example.fuzzers.core-fuzzer/square] - DEDUP_TOKEN: 262482a3bb5c872e
[example.fuzzers.core-fuzzer/square] - == libFuzzer crashing input ==
[example.fuzzers.core-fuzzer/square] - MS: 0 ; base unit: 0000000000000000000000000000000000000000
[example.fuzzers.core-fuzzer/square] - 0x2,
[example.fuzzers.core-fuzzer/square] - \002
[example.fuzzers.core-fuzzer/square] - artifact_prefix='fuzz/crashes/example/fuzzers/core_fuzzer/square/'; Test unit written to fuzz/crashes/example/fuzzers/core_fuzzer/square/crash-c4ea21bb365bbeeaf5f2c654883e56d11e43c44e
[example.fuzzers.core-fuzzer/square] - Base64: Ag==
[example.fuzzers.core-fuzzer/square] - INFO: __sanitizer_symbolize_pc or __sanitizer_get_module_and_offset_for_pc is not available, not printing coverage
[example.fuzzers.core-fuzzer/square] - INFO: __sanitizer_symbolize_pc or __sanitizer_get_module_and_offset_for_pc is not available, not printing coverage
[example.fuzzers.core-fuzzer/square] -   [  0 c9ee5681d3c59f7541c27a38b67edf46259e187b] sz:     1 runs:     0 succ:     0 focus: 0
[example.fuzzers.core-fuzzer/square] - stat::number_of_executed_units: 3
[example.fuzzers.core-fuzzer/square] - stat::average_exec_per_sec:     0
[example.fuzzers.core-fuzzer/square] - stat::new_units_added:          0
[example.fuzzers.core-fuzzer/square] - stat::slowest_unit_time_sec:    0
[example.fuzzers.core-fuzzer/square] - stat::peak_rss_mb:              742
[example.fuzzers.core-fuzzer/square] - reproducer_path='fuzz/reproducers/example/fuzzers/core_fuzzer/square/'; Java reproducer written to fuzz/reproducers/example/fuzzers/core_fuzzer/square/Crash_c4ea21bb365bbeeaf5f2c654883e56d11e43c44e.java
[example.fuzzers.core-fuzzer/square] -
[example.fuzzers.core-fuzzer/square] - #4       pulse  cov: 4 ft: 9 corp: 2/2b exec/s: 0 rss: 742Mb
[example.fuzzers.core-fuzzer/square] - #8       pulse  cov: 4 ft: 9 corp: 2/2b exec/s: 0 rss: 742Mb
[example.fuzzers.core-fuzzer/square] - #16      pulse  cov: 4 ft: 9 corp: 2/2b exec/s: 1 rss: 742Mb
[example.fuzzers.core-fuzzer/square] - #32      pulse  cov: 4 ft: 9 corp: 2/2b exec/s: 2 rss: 742Mb
[example.fuzzers.core-fuzzer/square] - #64      pulse  cov: 4 ft: 9 corp: 2/2b exec/s: 5 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:10.000 - #106        INITED cov: 4 ft: 9 corp: 2/2b exec/s: 9 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.993 - #128        pulse  cov: 4 ft: 9 corp: 2/2b lim: 4 exec/s: 11 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.993 - #256        pulse  cov: 4 ft: 9 corp: 2/2b lim: 6 exec/s: 23 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.993 - #512        pulse  cov: 4 ft: 9 corp: 2/2b lim: 8 exec/s: 46 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.988 - #1024       pulse  cov: 4 ft: 9 corp: 2/2b lim: 14 exec/s: 93 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.980 - #2048       pulse  cov: 4 ft: 9 corp: 2/2b lim: 21 exec/s: 186 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.967 - #4096       pulse  cov: 4 ft: 9 corp: 2/2b lim: 43 exec/s: 372 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.940 - #8192       pulse  cov: 4 ft: 9 corp: 2/2b lim: 80 exec/s: 744 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.904 - #16384      pulse  cov: 4 ft: 9 corp: 2/2b lim: 163 exec/s: 1489 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:09.803 - #32768      pulse  cov: 4 ft: 9 corp: 2/2b lim: 325 exec/s: 2978 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:08.661
[example.fuzzers.core-fuzzer/square] 00:00:08.468 - #65536      pulse  cov: 4 ft: 9 corp: 2/2b lim: 652 exec/s: 5957 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:07.898 - #131072     pulse  cov: 4 ft: 9 corp: 2/2b lim: 1300 exec/s: 10922 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:06.655
[example.fuzzers.core-fuzzer/square] 00:00:05.872 - #262144     pulse  cov: 4 ft: 9 corp: 2/2b lim: 2611 exec/s: 20164 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:04.654
[example.fuzzers.core-fuzzer/square] 00:00:02.654
[example.fuzzers.core-fuzzer/square] 00:00:01.856 - #524288     pulse  cov: 4 ft: 9 corp: 2/2b lim: 4096 exec/s: 34952 rss: 742Mb
[example.fuzzers.core-fuzzer/square] 00:00:00.651
[example.fuzzers.core-fuzzer/square] 00:00:00.000 - TIMEOUT
[example.fuzzers.core-fuzzer/square] Started at: 2025-05-16T00:20:17.543990
[example.fuzzers.core-fuzzer/square] Finished at: 2025-05-16T00:20:38.023347
[example.fuzzers.core-fuzzer/square] Lead time: 00:00:20.479
[example.fuzzers.core-fuzzer/square] Exit code: 77


Fuzzing has been completed for 1 target(s) in 1 namespace(s):
  - example.fuzzers.core-fuzzer/square (00:00:20.479)

Started at: 2025-05-16T00:20:17.537660
Finished at: 2025-05-16T00:20:38.023658
Total lead time: 00:00:20.485
```
