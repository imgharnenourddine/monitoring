package ma.imgharn.ensiasd;

/**
 * Application entry point.
 */
public final class Main {

    private Main() {
        // Utility class.
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnvironment();
        PageMonitor pageMonitor = new PageMonitor(
                config,
                new HashService(),
                new StateRepository(),
                new TelegramService(config),
                new EmailService(config)
        );

        pageMonitor.checkForChanges();
    }
}
