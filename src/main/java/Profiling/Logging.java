package src.main.java.Profiling;

import java.io.IOException;

public class Logging {
    public static void log(String method, long startTime, long stopTime) throws IOException {
        String res = method + " : " + (stopTime - startTime) +"\n";

        try {
            SDRaytracerAgent.output.write(res);
            SDRaytracerAgent.output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
