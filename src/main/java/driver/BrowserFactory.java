package driver;

import io.github.bonigarcia.wdm.DriverManagerType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.naming.NamingException;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Collectors;

final public class BrowserFactory {

    public enum BrowserType {

        CHROME {
            @Override
            public WebDriver getWebDriver() {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("headless");
                return new ChromeDriver(options);
            }
        };

        public abstract WebDriver getWebDriver();
    }

    private static final EnumMap<BrowserType, DriverManagerType> driverManagerMap = new EnumMap<BrowserType, DriverManagerType>(BrowserType.class) {{
        put(BrowserType.CHROME, DriverManagerType.CHROME);
    }};

    private BrowserFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static WebDriver setUp(final String type) throws NamingException {
        Set<String> driverNames = driverManagerMap.keySet().stream().map(Enum::name).collect(Collectors.toSet());
        if (driverNames.contains(type)) {
            BrowserType browserType = BrowserType.valueOf(type);
            WebDriverManager.getInstance(driverManagerMap.get(browserType)).setup();
            return browserType.getWebDriver();
        }
        throw new NamingException(String.format("Wrong Browser Name: %s", type));
    }
}
