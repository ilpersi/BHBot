import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Extended Logger interface with convenience methods for
 * the STATS, READOUT, AUTOSHRINE, AUTOREVIVE and AUTORUNE custom log levels.
 * <p>Compatible with Log4j 2.6 or higher.</p>
 */
public final class BHBotLogger extends ExtendedLoggerWrapper {
    private static final long serialVersionUID = 918354644352989L;
    private final ExtendedLoggerWrapper logger;

    private static final String FQCN = BHBotLogger.class.getName();
    private static final Level STATS = Level.forName("STATS", 390);
    private static final Level READOUT = Level.forName("READOUT", 395);
    private static final Level AUTOSHRINE = Level.forName("AUTOSHRINE", 399);
    private static final Level AUTOREVIVE = Level.forName("AUTOREVIVE", 399);
    private static final Level AUTOBRIBE = Level.forName("AUTOBRIBE", 399);
    private static final Level AUTORUNE = Level.forName("AUTORUNE", 399);

    private BHBotLogger(final Logger logger) {
        super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
        this.logger = this;
    }

    /**
     * Returns a custom Logger with the name of the calling class.
     *
     * @return The custom Logger for the calling class.
     */
    public static BHBotLogger create() {
        final Logger wrapped = LogManager.getLogger();
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger using the fully qualified name of the Class as
     * the Logger name.
     *
     * @param loggerName The Class whose name should be used as the Logger name.
     *            If null it will default to the calling class.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final Class<?> loggerName) {
        final Logger wrapped = LogManager.getLogger(loggerName);
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger using the fully qualified name of the Class as
     * the Logger name.
     *
     * @param loggerName The Class whose name should be used as the Logger name.
     *            If null it will default to the calling class.
     * @param messageFactory The message factory is used only when creating a
     *            logger, subsequent use does not change the logger but will log
     *            a warning if mismatched.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final Class<?> loggerName, final MessageFactory messageFactory) {
        final Logger wrapped = LogManager.getLogger(loggerName, messageFactory);
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger using the fully qualified class name of the value
     * as the Logger name.
     *
     * @param value The value whose class name should be used as the Logger
     *            name. If null the name of the calling class will be used as
     *            the logger name.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final Object value) {
        final Logger wrapped = LogManager.getLogger(value);
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger using the fully qualified class name of the value
     * as the Logger name.
     *
     * @param value The value whose class name should be used as the Logger
     *            name. If null the name of the calling class will be used as
     *            the logger name.
     * @param messageFactory The message factory is used only when creating a
     *            logger, subsequent use does not change the logger but will log
     *            a warning if mismatched.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final Object value, final MessageFactory messageFactory) {
        final Logger wrapped = LogManager.getLogger(value, messageFactory);
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will
     *            be used.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final String name) {
        final Logger wrapped = LogManager.getLogger(name);
        return new BHBotLogger(wrapped);
    }

    /**
     * Returns a custom Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will
     *            be used.
     * @param messageFactory The message factory is used only when creating a
     *            logger, subsequent use does not change the logger but will log
     *            a warning if mismatched.
     * @return The custom Logger.
     */
    public static BHBotLogger create(final String name, final MessageFactory messageFactory) {
        final Logger wrapped = LogManager.getLogger(name, messageFactory);
        return new BHBotLogger(wrapped);
    }

    /**
     * Logs a message with the specific Marker at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void stats(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, STATS, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void stats(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void stats(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, STATS, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, STATS, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void stats(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, message, t);
    }

    /**
     * Logs a message at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, message, t);
    }

    /**
     * Logs a message object with the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void stats(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, STATS, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void stats(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, STATS, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, STATS, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void stats(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code STATS} level.
     *
     * @param msg the message string to be logged
     */
    public void stats(final Message msg) {
        logger.logIfEnabled(FQCN, STATS, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code STATS} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void stats(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, msg, t);
    }

    /**
     * Logs a message object with the {@code STATS} level.
     *
     * @param message the message object to log.
     */
    public void stats(final Object message) {
        logger.logIfEnabled(FQCN, STATS, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void stats(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code STATS} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void stats(final CharSequence message) {
        logger.logIfEnabled(FQCN, STATS, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void stats(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, message, t);
    }

    /**
     * Logs a message object with the {@code STATS} level.
     *
     * @param message the message object to log.
     */
    public void stats(final String message) {
        logger.logIfEnabled(FQCN, STATS, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void stats(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, STATS, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void stats(final String message, final Object p0, final Object p1, final Object p2,
                      final Object p3, final Object p4, final Object p5, final Object p6,
                      final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, STATS, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code STATS} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void stats(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code STATS}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void stats(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, STATS, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code STATS}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void stats(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code STATS} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void stats(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, STATS, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code STATS} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void stats(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, STATS, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code STATS}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void stats(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code STATS} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void stats(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, STATS, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code STATS} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void stats(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, STATS, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code STATS}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void stats(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code STATS} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void stats(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, STATS, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code STATS}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void stats(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, STATS, null, msgSupplier, t);
    }

    /**
     * Logs a message with the specific Marker at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void readout(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, READOUT, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void readout(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void readout(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void readout(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, t);
    }

    /**
     * Logs a message at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, t);
    }

    /**
     * Logs a message object with the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void readout(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void readout(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void readout(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code READOUT} level.
     *
     * @param msg the message string to be logged
     */
    public void readout(final Message msg) {
        logger.logIfEnabled(FQCN, READOUT, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code READOUT} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void readout(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, msg, t);
    }

    /**
     * Logs a message object with the {@code READOUT} level.
     *
     * @param message the message object to log.
     */
    public void readout(final Object message) {
        logger.logIfEnabled(FQCN, READOUT, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void readout(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code READOUT} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void readout(final CharSequence message) {
        logger.logIfEnabled(FQCN, READOUT, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void readout(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, message, t);
    }

    /**
     * Logs a message object with the {@code READOUT} level.
     *
     * @param message the message object to log.
     */
    public void readout(final String message) {
        logger.logIfEnabled(FQCN, READOUT, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void readout(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, READOUT, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void readout(final String message, final Object p0, final Object p1, final Object p2,
                        final Object p3, final Object p4, final Object p5, final Object p6,
                        final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, READOUT, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code READOUT} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void readout(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code READOUT}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void readout(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, READOUT, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code READOUT}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void readout(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code READOUT} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void readout(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, READOUT, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code READOUT} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void readout(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, READOUT, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code READOUT}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void readout(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code READOUT} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void readout(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, READOUT, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code READOUT} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void readout(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, READOUT, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code READOUT}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void readout(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code READOUT} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void readout(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, READOUT, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code READOUT}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void readout(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, READOUT, null, msgSupplier, t);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void autoshrine(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autoshrine(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autoshrine(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autoshrine(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, t);
    }

    /**
     * Logs a message at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autoshrine(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autoshrine(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autoshrine(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code AUTOSHRINE} level.
     *
     * @param msg the message string to be logged
     */
    public void autoshrine(final Message msg) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code AUTOSHRINE} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autoshrine(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOSHRINE} level.
     *
     * @param message the message object to log.
     */
    public void autoshrine(final Object message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autoshrine(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOSHRINE} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autoshrine(final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autoshrine(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOSHRINE} level.
     *
     * @param message the message object to log.
     */
    public void autoshrine(final String message) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autoshrine(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autoshrine(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOSHRINE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autoshrine(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code AUTOSHRINE}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autoshrine(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOSHRINE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autoshrine(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOSHRINE} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autoshrine(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code AUTOSHRINE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autoshrine(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOSHRINE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autoshrine(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code AUTOSHRINE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autoshrine(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOSHRINE} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autoshrine(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOSHRINE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autoshrine(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOSHRINE} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autoshrine(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOSHRINE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autoshrine(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOSHRINE, null, msgSupplier, t);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void autorevive(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autorevive(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autorevive(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorevive(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, t);
    }

    /**
     * Logs a message at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autorevive(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autorevive(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorevive(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code AUTOREVIVE} level.
     *
     * @param msg the message string to be logged
     */
    public void autorevive(final Message msg) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code AUTOREVIVE} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autorevive(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOREVIVE} level.
     *
     * @param message the message object to log.
     */
    public void autorevive(final Object message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorevive(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOREVIVE} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autorevive(final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autorevive(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOREVIVE} level.
     *
     * @param message the message object to log.
     */
    public void autorevive(final String message) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autorevive(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorevive(final String message, final Object p0, final Object p1, final Object p2,
                           final Object p3, final Object p4, final Object p5, final Object p6,
                           final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOREVIVE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorevive(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code AUTOREVIVE}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autorevive(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOREVIVE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autorevive(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOREVIVE} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autorevive(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code AUTOREVIVE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autorevive(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOREVIVE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autorevive(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code AUTOREVIVE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autorevive(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOREVIVE} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autorevive(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOREVIVE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autorevive(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOREVIVE} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autorevive(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOREVIVE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autorevive(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOREVIVE, null, msgSupplier, t);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void autorune(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autorune(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autorune(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorune(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, t);
    }

    /**
     * Logs a message at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, t);
    }

    /**
     * Logs a message object with the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autorune(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autorune(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorune(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code AUTORUNE} level.
     *
     * @param msg the message string to be logged
     */
    public void autorune(final Message msg) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code AUTORUNE} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autorune(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTORUNE} level.
     *
     * @param message the message object to log.
     */
    public void autorune(final Object message) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorune(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code AUTORUNE} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autorune(final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autorune(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, t);
    }

    /**
     * Logs a message object with the {@code AUTORUNE} level.
     *
     * @param message the message object to log.
     */
    public void autorune(final String message) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autorune(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autorune(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTORUNE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autorune(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code AUTORUNE}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autorune(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTORUNE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autorune(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTORUNE} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autorune(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code AUTORUNE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autorune(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTORUNE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autorune(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code AUTORUNE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autorune(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTORUNE} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autorune(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTORUNE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autorune(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTORUNE} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autorune(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTORUNE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autorune(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTORUNE, null, msgSupplier, t);
    }
    /**
     * Logs a message with the specific Marker at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    public void autobribe(final Marker marker, final Message msg) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autobribe(final Marker marker, final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autobribe(final Marker marker, final Object message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autobribe(final Marker marker, final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, t);
    }

    /**
     * Logs a message at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message object to log.
     */
    public void autobribe(final Marker marker, final String message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autobribe(final Marker marker, final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autobribe(final Marker marker, final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@code AUTOBRIBE} level.
     *
     * @param msg the message string to be logged
     */
    public void autobribe(final Message msg) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msg, (Throwable) null);
    }

    /**
     * Logs the specified Message at the {@code AUTOBRIBE} level.
     *
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    public void autobribe(final Message msg, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msg, t);
    }

    /**
     * Logs a message object with the {@code AUTOBRIBE} level.
     *
     * @param message the message object to log.
     */
    public void autobribe(final Object message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, (Throwable) null);
    }

    /**
     * Logs a message at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autobribe(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, t);
    }

    /**
     * Logs a message CharSequence with the {@code AUTOBRIBE} level.
     *
     * @param message the message CharSequence to log.
     * @since Log4j-2.6
     */
    public void autobribe(final CharSequence message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, (Throwable) null);
    }

    /**
     * Logs a CharSequence at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the CharSequence to log.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.6
     */
    public void autobribe(final CharSequence message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, t);
    }

    /**
     * Logs a message object with the {@code AUTOBRIBE} level.
     *
     * @param message the message object to log.
     */
    public void autobribe(final String message) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params parameters to the message.
     * @see #getMessageFactory()
     */
    public void autobribe(final String message, final Object... params) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, params);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4, p5);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Logs a message with parameters at the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     * @param p3 parameter to the message.
     * @param p4 parameter to the message.
     * @param p5 parameter to the message.
     * @param p6 parameter to the message.
     * @param p7 parameter to the message.
     * @param p8 parameter to the message.
     * @param p9 parameter to the message.
     * @see #getMessageFactory()
     * @since Log4j-2.6
     */
    public void autobribe(final String message, final Object p0, final Object p1, final Object p2,
                         final Object p3, final Object p4, final Object p5, final Object p6,
                         final Object p7, final Object p8, final Object p9) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs a message at the {@code AUTOBRIBE} level including the stack trace of
     * the {@link Throwable} {@code t} passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    public void autobribe(final String message, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the {@code AUTOBRIBE}level.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autobribe(final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOBRIBE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autobribe(final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOBRIBE} level with the specified Marker.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @since Log4j-2.4
     */
    public void autobribe(final Marker marker, final Supplier<?> msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the
     * {@code AUTOBRIBE} level.
     *
     * @param marker the marker data specific to this log statement
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autobribe(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, message, paramSuppliers);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOBRIBE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message;
     *            the format depends on the message factory.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autobribe(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msgSupplier, t);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is
     * the {@code AUTOBRIBE} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     * @since Log4j-2.4
     */
    public void autobribe(final String message, final Supplier<?>... paramSuppliers) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, message, paramSuppliers);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOBRIBE} level with the specified Marker. The {@code MessageSupplier} may or may
     * not use the {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autobribe(final Marker marker, final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOBRIBE}
     * level) with the specified Marker and including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param marker the marker data specific to this log statement
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t A Throwable or null.
     * @since Log4j-2.4
     */
    public void autobribe(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, marker, msgSupplier, t);
    }

    /**
     * Logs a message which is only to be constructed if the logging level is the
     * {@code AUTOBRIBE} level. The {@code MessageSupplier} may or may not use the
     * {@link MessageFactory} to construct the {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @since Log4j-2.4
     */
    public void autobribe(final MessageSupplier msgSupplier) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msgSupplier, (Throwable) null);
    }

    /**
     * Logs a message (only to be constructed if the logging level is the {@code AUTOBRIBE}
     * level) including the stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * The {@code MessageSupplier} may or may not use the {@link MessageFactory} to construct the
     * {@code Message}.
     *
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     * @since Log4j-2.4
     */
    public void autobribe(final MessageSupplier msgSupplier, final Throwable t) {
        logger.logIfEnabled(FQCN, AUTOBRIBE, null, msgSupplier, t);
    }
}

