import driver.Browser;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunTestNGResultsParserToXls {

    private static final By FAILED_TESTS_NAMES_LOCATOR = By.xpath("//div[@class='stacktrace']/preceding::h2[@id]");
    private static final By FAILED_TESTS_STACKTRACE_LOCATOR = By.xpath("//h2[@id]//following-sibling::*//div[@class='stacktrace']");
    private static final String FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH = "//table[@class='invocation-failed']//tr//td[@title]";
    private static final By FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH);
    private static final By FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_LOCATOR = By.xpath(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_XPATH + "/following-sibling::td[.//pre]");

    public static final String EXCEL_EXTENSION = "xlsx";

    public static void main(String[] args) {
        JFileChooser jFileChooser = viewFileChooser();
        File file = jFileChooser.getSelectedFile();

        //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
//        String reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\Parser-TestNG-xml-of-Results-to-xls-for-filtering\\RegressionSuiteFull.html";
        String reportTestNGPath = file.getAbsolutePath();

        File reportParsedFile = null;
        try {
            reportParsedFile = getTestReportParsedFile(reportTestNGPath);
        } catch (Exception e) {
            Browser.getInstance().exit();
            viewAlert(e.getMessage());
            e.printStackTrace();
        }
        viewAlert(String.format("Excel file PATH: %s", Objects.requireNonNull(reportParsedFile).getPath()));
    }

    private static JFileChooser viewFileChooser() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(getDecodeAbsolutePath(getSourcePath())));
        jFileChooser.showOpenDialog(null);
        return jFileChooser;
    }

    public static File getTestReportParsedFile(String reportPath) {
        Browser.getInstance();
        WebDriver driver = Browser.getDriver();
        driver.get(reportPath);

        List<String> failedTestsNames;
        List<String> failedTestsStacktraces;
        failedTestsNames = getTextElements(driver, FAILED_TESTS_NAMES_LOCATOR);
        if (failedTestsNames.size() == 0) {
            failedTestsNames = driver.findElements(FAILED_TESTS_NAMES_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                    .map(test -> test.getAttribute("title"))
                    .collect(Collectors.toList());
            failedTestsStacktraces = driver.findElements(FAILED_TESTS_STACKTRACE_JENKINS_PLUGIN_REPORT_LOCATOR).stream()
                    .map(stacktrace -> stacktrace.getText()
                            .replace("Click to show all stack frames", ""))
                    .collect(Collectors.toList());
        } else {
            failedTestsStacktraces = getTextElements(driver, FAILED_TESTS_STACKTRACE_LOCATOR);
        }

        Map<String, List<String>> reportSummary = SummaryReport.groupingTestsFailed(failedTestsNames, failedTestsStacktraces);

        Browser.getInstance().exit();

        summaryXlsReport(reportPath, reportSummary);
        return fetchXlsReport(reportPath, failedTestsNames, failedTestsStacktraces);
    }

    private static void viewAlert(String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    private static List<String> getTextElements(WebDriver driver, By locator) {
        return driver.findElements(locator).stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public static String getDecodeAbsolutePath(String sourcePath) {
        return decode(new File(sourcePath).getAbsolutePath());
    }

    private static String getSourcePath() {
        return RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public static String decode(String path) {
        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            viewAlert(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static File fetchXlsReport(String reportTestNGPath, List<String> failedTestsNames,
                                       List<String> failedTestsStacktrace) {
        String generateFileName = getGenerateReportFileName(reportTestNGPath, failedTestsNames.size(),
                failedTestsStacktrace.size(), EXCEL_EXTENSION);

        return createFile(generateFileName, failedTestsNames, failedTestsStacktrace);
    }

    private static File summaryXlsReport(String reportTestNGPath, Map<String, List<String>> mapTests) {
        String generateFileName = getGenerateSummaryReportFileName(reportTestNGPath, EXCEL_EXTENSION);

        List<String> failedMethods = new ArrayList<>(mapTests.keySet());
        List<String> countFailed = new ArrayList<>();
        List<String> columnTestsCells = new ArrayList<>();

        for(String failedMethod : failedMethods){
            String columnTestsCell = "";

            countFailed.add(String.valueOf(mapTests.get(failedMethod).size()));
            List<String> testsList = mapTests.get(failedMethod);

            for(String test : testsList){
                columnTestsCell = columnTestsCell.concat(test).concat("\r\n");
            }

            columnTestsCells.add(columnTestsCell);
        }

        return createFile(generateFileName, countFailed, failedMethods, columnTestsCells);
    }

    private static File createFile(String fileName, List<String>... columnLists) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("report");
        for (int rowNum = 0; rowNum < columnLists[0].size(); ++rowNum) {
            Row row = sheet.createRow(rowNum);

            for(int columnNum = 0; columnNum < columnLists.length; ++columnNum){
                Cell cell = row.createCell(columnNum);
                cell.setCellValue(columnLists[columnNum].get(rowNum));
            }
        }

        File excelFile = getGenerateReportFile(fileName);
        try (FileOutputStream out = new FileOutputStream(excelFile)) {
            workbook.write(out);
        } catch (Exception e) {
            viewAlert(e.getMessage());
            e.printStackTrace();
        }
        return excelFile;
    }

    private static String getGenerateReportFileName(String reportTestNGPath, int failedTestsNamesCount, int failedTestsStacktraceCount, String extension) {
        return String.format("%s_%dTests_%dStacktrace.%s",
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()),
                failedTestsNamesCount,
                failedTestsStacktraceCount,
                extension);
    }

    private static String getGenerateSummaryReportFileName(String reportTestNGPath, String extension) {
        return String.format("%s_summary.%s",
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()),
                extension);
    }

    private static File getGenerateReportFile(String generateFileName) {
        return new File(new File(getDecodeAbsolutePath(getSourcePath())).getParent() + File.separator + generateFileName);
    }
}
