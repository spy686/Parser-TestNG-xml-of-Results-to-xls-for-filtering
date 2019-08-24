import io.github.bonigarcia.wdm.DriverManagerType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;

public class RunTestNGResultsParserToXls {

    private static final By FAILED_TESTS_NAMES_LOCATOR = By.xpath("//div[@class='stacktrace']/preceding::h2[@id]");
    private static final By FAILED_TESTS_STACKTRACES_LOCATOR = By.xpath("//h2[@id]//following-sibling::*//div[@class='stacktrace']");
    private static final String FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH = "//table[@class='invocation-failed']//tr//td[@title]";
    private static final By FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH);
    private static final By FAILED_TESTS_STACKTRACES_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH + "/following-sibling::td[.//pre]");

    public static void main(String[] args) throws UnsupportedEncodingException {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getProjectDir()));
        jFileChooser.showOpenDialog(null);
        File file = jFileChooser.getSelectedFile();

        //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
//        String reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\RegressionSuiteFull.html";
        String reportTestNGPath = file.getAbsolutePath();

        WebDriver driver = initChromeDriver();
        try {
            driver.get(reportTestNGPath);

            List<String> failedTestsNames;
            List<String> failedTestsStacktraces;
            failedTestsNames = getTextElements(driver, FAILED_TESTS_NAMES_LOCATOR);
            if (failedTestsNames.size() == 0) {
                failedTestsNames = driver.findElements(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                        .map(test -> test.getAttribute("title")).collect(Collectors.toList());
                failedTestsStacktraces = driver.findElements(FAILED_TESTS_STACKTRACES_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                        .map(stacktrace -> stacktrace.getText().replace("Click to show all stack frames", "")).collect(Collectors.toList());
            } else {
                failedTestsStacktraces = getTextElements(driver, FAILED_TESTS_STACKTRACES_LOCATOR);
            }
            fetchXlsReport(reportTestNGPath, failedTestsNames, failedTestsStacktraces);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        } finally {
            driver.quit();
        }
    }

    private static WebDriver initChromeDriver() {
        WebDriverManager.getInstance(DriverManagerType.CHROME).setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        return new ChromeDriver(options);
    }

    private static List<String> getTextElements(WebDriver driver, By locator) {
        return driver.findElements(locator).stream().map(WebElement::getText).collect(Collectors.toList());
    }

    private static String getProjectDir() throws UnsupportedEncodingException {
        return URLDecoder.decode(new File(RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation()
                .getPath()).getPath() + File.separator, "UTF-8");
    }

    private static void fetchXlsReport(String reportTestNGPath, List<String> failedTestsNames,
                                       List<String> failedTestsStacktraces) throws UnsupportedEncodingException {
        int failedTestsNamesCount = failedTestsNames.size();
        System.out.println(String.format("Was found %d tests names", failedTestsNamesCount));
        int failedTestsStacktracesCount = failedTestsStacktraces.size();
        System.out.println(String.format("Was found %d tests stacktraces", failedTestsStacktracesCount));

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("report");
        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);
            Cell cell1 = row.createCell(0);
            cell1.setCellValue(failedTestsNames.get(rowNum));
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(failedTestsStacktraces.get(rowNum));
        }

        String fileNameXls = String.format("%s_%dTs_%dStrs.%s",
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()),
                failedTestsNamesCount,
                failedTestsStacktracesCount,
                "xlsx");
        File excelFile = new File(new File(getProjectDir()).getParent() + File.separator + fileNameXls);
        System.out.println(String.format("Excel file PATH: %s", excelFile.getPath()));
        try (FileOutputStream out = new FileOutputStream(excelFile)) {
            workbook.write(out);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, String.format("Excel file PATH: %s", excelFile.getPath()));
    }
}
