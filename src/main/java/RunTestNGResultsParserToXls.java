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

    private static final By failedTestsNamesLocator = By.xpath("//div[@class='stacktrace']/preceding::h2[@id]");
    private static final By failedTestsStacktracesLocator = By.xpath("//h2[@id]//following-sibling::*//div[@class='stacktrace']");

    public static void main(String[] args) throws UnsupportedEncodingException {
        String projectDir = URLDecoder.decode(new File(RunTestNGResultsParserToXls.class.getProtectionDomain().getCodeSource().getLocation()
                .getPath()).getPath() + File.separator, "UTF-8");

        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose a report");
        jFileChooser.setCurrentDirectory(new File(projectDir));
        jFileChooser.showOpenDialog(null);
        File file = jFileChooser.getSelectedFile();

        //открываем репорт в браузере и покируем путь из строки поиска или просто путь к файлу
//        String reportTestNGPath = "C:\\Users\\Xiaomi\\Google Диск\\popo\\java\\parse_testng_results\\report.html";
        String reportTestNGPath = file.getAbsolutePath();

        WebDriverManager.getInstance(DriverManagerType.CHROME).setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);
        driver.get(reportTestNGPath);

        List<String> failedTestsNames = driver.findElements(failedTestsNamesLocator).stream().map(WebElement::getText).collect(Collectors.toList());
        int failedTestsNamesCount = failedTestsNames.size();
        System.out.println(String.format("Was found %d tests names", failedTestsNamesCount));
        List<String> failedTestsStacktraces = driver.findElements(failedTestsStacktracesLocator).stream().map(WebElement::getText).collect(Collectors.toList());
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
                FilenameUtils.removeExtension(new File(reportTestNGPath).getName()) + File.separator,
                failedTestsNamesCount,
                failedTestsStacktracesCount,
                "xlsx");
        try (FileOutputStream out = new FileOutputStream(new File(new File(projectDir).getParent() + fileNameXls))) {
            workbook.write(out);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

        driver.quit();
    }
}
