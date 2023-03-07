package org.apache.logging.log4j.core.async;

public class AsyncLoggerConfig {

    public static class RootLogger {

        public static class Builder<B extends Builder<B>> extends RootLogger.Builder<B> {

            @Override
            public LoggerConfig build() {
                LevelAndRefs container = LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(),
                        getConfig());
                return new AsyncLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            }
        }
    }
}
