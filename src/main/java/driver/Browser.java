package driver;

import org.openqa.selenium.WebDriver;

import javax.naming.NamingException;

public final class Browser {

    private static final String CURRENT_BROWSER = "CHROME";
    private static ThreadLocal<WebDriver> driverHolder = ThreadLocal.withInitial(Browser::getNewDriver);
    private static Browser instance = new Browser();

    private Browser() {
    }

    public static Browser getInstance() {
        if (instance == null) {
            synchronized (Browser.class) {
                if (instance == null) {
                    instance = new Browser();
                }
            }
        }
        return instance;
    }

    public static WebDriver getDriver() {
        if (driverHolder.get() == null) {
            driverHolder.set(getNewDriver());
        }
        return driverHolder.get();
    }

    public void exit() {
        try {
            getDriver().quit();
        } finally {
            if (isBrowserAlive()) {
                driverHolder.set(null);
            }
        }
    }

    private boolean isBrowserAlive() {
        return driverHolder.get() != null;
    }

    public void windowMaximise() {
        getDriver().manage().window().maximize();
    }

    private static WebDriver getNewDriver() {
        try {
            return BrowserFactory.setUp(CURRENT_BROWSER);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
