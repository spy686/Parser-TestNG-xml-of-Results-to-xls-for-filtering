import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryReport {

    public static Map<String, List<String>> groupingTestsFailed(List<String> failedTestsNames, List<String> failedTestsStacktrace) {
        Map<String, List<String>> testsMap = new HashMap<>();

        for (int rowNum = 0; rowNum < failedTestsNames.size(); ++rowNum) {
            String failedTest = failedTestsNames.get(rowNum).replace(":", ": ");
            String failedMethod = getMethod(failedTestsNames.get(rowNum), failedTestsStacktrace.get(rowNum));

            List<String> actualList;
            if(testsMap.get(failedMethod)==null){
                actualList = new ArrayList<>();
            }else{
                actualList = testsMap.get(failedMethod);
            }
            actualList.add(failedTest);
            testsMap.put(failedMethod, actualList);

        }

        return testsMap;
    }

    private static String getMethod(String failedTest, String stackTrace){
        String testName = failedTest.split(":")[1];

        List<String> stackTraceStringsList = Arrays.asList(stackTrace.split("\\n"));
        int testStringNum = getNumberString(testName, stackTraceStringsList);

        String[] failedMethodArray = stackTraceStringsList.get(testStringNum-1).split("\\.");
        String failedPage = failedMethodArray[failedMethodArray.length - 3];
        String failedMethod = failedMethodArray[failedMethodArray.length - 2].split("\\(")[0];

        return String.format("%s.%s", failedPage, failedMethod);
    }

    private static int getNumberString(String expectString, List<String> list){
        for (int rowNum = 0; rowNum < list.size(); ++rowNum) {
            if(list.get(rowNum).contains(expectString)){
                return rowNum;
            }
        }
        throw new RuntimeException(String.format("List does not contain '%s'.", expectString));
    }
}
