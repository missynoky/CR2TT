package pl.uwb.cr2tt.utils;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {

    private TimeUtils() {}

    public static String getExecutionTimeFormatted(Instant start, Instant end) {
        Duration duration = Duration.between(start, end);

        long totalTimeMs = duration.toMillis();

        if (totalTimeMs < 1000) {
            return "0 min 0 sec (" + totalTimeMs + " ms)";
        }

        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return hours + " h " + minutes + " min " + seconds + " sec";
        } else {
            return minutes + " min " + seconds + " sec";
        }
    }
}