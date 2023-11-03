package me.vadim.archive.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author SubOptimal
 * @link <a href="https://stackoverflow.com/a/26108654/12344841">Source</a>
 */
public class CustomFormatter extends Formatter {

    public static final String defaultFormat =  "%1tH:%<tM:%<tS %2$-7s %3$s (%4$s) %5$s%6$s%n";

    private final Date date = new Date();
    private final String format;

    public CustomFormatter() {
        this(defaultFormat);
    }

    public CustomFormatter(String customFormat) {
        this.format = customFormat;
    }

    @Override
    public String format(LogRecord record) {
        date.setTime(record.getMillis());
        String source = "";
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                             date,
                             record.getLevel().getName(),
                             record.getLoggerName(),
                             source,
                             message,
                             throwable);
    }
}