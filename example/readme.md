# example

This is a simple demo project to show you how to set up fuzzing correctly.

### Using `Clojure CLI`

```bash
# cleanup target
$ bb clean

# compile fuzzers
$ bb fuzz:compile

# Fuzzing targets
$ bb fuzz --timeout 10s
```

### Using `Leiningen`

```bash
# cleanup target
$ lein fuzz:clean

# compile fuzzers
$ lein fuzz:compile

# Fuzzing targets
$ lein fuzz -- --timeout 10s
```
