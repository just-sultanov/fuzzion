import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Crash_9b99593353a610c4bee0d6a94a01a3296080c0fb {
    static final String base64Bytes = String.join("", "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAABdwQAAAABc3IADmphdmEubGFuZy5Mb25nO4vkkMyPI98CAAFKAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAAAAAACeA==");

    public static void main(String[] args) throws Throwable {
        Crash_9b99593353a610c4bee0d6a94a01a3296080c0fb.class.getClassLoader().setDefaultAssertionStatus(true);
        try {
            Method fuzzerInitialize = example.core_fuzzer.Square.class.getMethod("fuzzerInitialize");
            fuzzerInitialize.invoke(null);
        } catch (NoSuchMethodException ignored) {
            try {
                Method fuzzerInitialize = example.core_fuzzer.Square.class.getMethod("fuzzerInitialize", String[].class);
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
        example.core_fuzzer.Square.fuzzerTestOneInput(input);
    }
}