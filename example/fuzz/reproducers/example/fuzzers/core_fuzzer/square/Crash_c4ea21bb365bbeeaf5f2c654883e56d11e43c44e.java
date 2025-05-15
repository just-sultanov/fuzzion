import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Crash_c4ea21bb365bbeeaf5f2c654883e56d11e43c44e {
    static final String base64Bytes = String.join("", "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAABdwQAAAABc3IADmphdmEubGFuZy5Mb25nO4vkkMyPI98CAAFKAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAAAAAACeA==");

    public static void main(String[] args) throws Throwable {
        Crash_c4ea21bb365bbeeaf5f2c654883e56d11e43c44e.class.getClassLoader().setDefaultAssertionStatus(true);
        try {
            Method fuzzerInitialize = example.fuzzers.core_fuzzer.Square.class.getMethod("fuzzerInitialize");
            fuzzerInitialize.invoke(null);
        } catch (NoSuchMethodException ignored) {
            try {
                Method fuzzerInitialize = example.fuzzers.core_fuzzer.Square.class.getMethod("fuzzerInitialize", String[].class);
                fuzzerInitialize.invoke(null, (Object) args);
            } catch (NoSuchMethodException ignored1) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        }
        com.code_intelligence.jazzer.api.CannedFuzzedDataProvider input = new com.code_intelligence.jazzer.api.CannedFuzzedDataProvider(base64Bytes);
        example.fuzzers.core_fuzzer.Square.fuzzerTestOneInput(input);
    }
}