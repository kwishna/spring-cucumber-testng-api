package org.spring.bdd.extent;


import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.util.List;

public class ExtentReporting {
    private static final ThreadLocal<ExtentReports> extent = new ThreadLocal<>();
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    public static void initExtent() {
        if (extent.get() == null) {
            ExtentReports ext = new ExtentReports();
            ExtentSparkReporter spark = new ExtentSparkReporter("build/Spark/Spark.html");
            ext.attachReporter(spark);
            extent.set(ext);
        }
    }

    public static void startTest(String testName) {
        if (extent.get() == null) {
            initExtent();
        }
        if (extent.get() != null && extentTest.get() == null) {
            ExtentReports ext = extent.get();
            ExtentTest test = ext.createTest(testName);
            extentTest.set(test);
        }
    }

    private static ExtentTest getCurrentTest() {
        ExtentTest t = extentTest.get();
        if (t == null) {
            startTest("TestName");
        }
        return t;
    }

    public static void addWarning(String warn) {
        if (getCurrentTest() != null) {
            getCurrentTest().warning(warn);
        }
    }

    public static void addPass(String warn) {
        if (getCurrentTest() != null) {
            getCurrentTest().pass(warn);
        }
    }

    public static void addFail(String warn) {
        if (getCurrentTest() != null) {
            getCurrentTest().fail(warn);
        }
    }

    public static void addFail(Throwable t) {
        if (getCurrentTest() != null) {
            getCurrentTest().fail(t);
        }
    }

    public static void codeBlock(String code, CodeLanguage lang) {
        if (getCurrentTest() != null) {
            getCurrentTest().pass(MarkupHelper.createCodeBlock(code, lang));
        }
    }

    public static void addScreenshot(String path, String info) {
        if (getCurrentTest() != null) {
            getCurrentTest().addScreenCaptureFromPath(path).pass(info);
        }
    }

    public static void addList(List<Object> items) {
        if (getCurrentTest() != null) {
            getCurrentTest().info(MarkupHelper.createOrderedList(items));
        }
    }

    public static void addLabel(String val, ExtentColor clr) {
        if (getCurrentTest() != null) {
            getCurrentTest().info(MarkupHelper.createLabel(val, clr));
        }
    }

    public static void addInfo(String info) {
        if (getCurrentTest() != null) {
            getCurrentTest().info(info);
        }
    }

    public static void addCategory(String category) {
        if (getCurrentTest() != null) {
            getCurrentTest().assignCategory(category);
        }
    }

    public static void addAuthor(String author) {
        if (getCurrentTest() != null) {
            getCurrentTest().assignAuthor(author);
        }
    }

    public static void addDevice(String device) {
        if (getCurrentTest() != null) {
            getCurrentTest().assignDevice(device);
        }
    }

    public static void createNode(String nodeName) {
        if (getCurrentTest() != null) {
            getCurrentTest().createNode(nodeName);
        }
    }

    public static void flush() {
        if (extent.get() != null) {
            extent.get().flush();
        }
    }

    public static void endTest() {
        extentTest.remove();
    }
}