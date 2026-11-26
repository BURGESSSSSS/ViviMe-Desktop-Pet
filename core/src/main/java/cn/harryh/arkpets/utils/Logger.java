/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.utils;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

/** Connects retained motion components to the JDK logger. */
public final class Logger {
    private Logger() {}

    public static void info(String category, String message) {
        System.getLogger(category).log(INFO, message);
    }

    public static void warn(String category, String message) {
        System.getLogger(category).log(WARNING, message);
    }
}
