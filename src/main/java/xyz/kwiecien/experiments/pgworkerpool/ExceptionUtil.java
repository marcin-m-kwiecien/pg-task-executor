package xyz.kwiecien.experiments.pgworkerpool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    public static String stackTraceToString(Throwable throwable) {
        try (var writer = new StringWriter();
             var printWriter = new PrintWriter(writer)) {
            throwable.printStackTrace(printWriter);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
