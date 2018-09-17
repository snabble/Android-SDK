package io.snabble.sdk;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EncodedCodesGeneratorTest extends SnabbleSdkTest {
    @Test
    @UiThreadTest
    public void testGeneration() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        generator.add("foo");
        generator.add("bar");
        generator.add("baz");

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("+foo;bar;baz-", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testSplitAtMaxChars() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .maxChars(13)
                .maxCodes(9999)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        generator.add("foo");
        generator.add("bar");
        generator.add("baz");
        generator.add("asdf");

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+foo;bar;baz-", codes.get(0));
        Assert.assertEquals("+asdf-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testSplitAtMaxCodes() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .maxCodes(3)
                .maxChars(2000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        generator.add("foo");
        generator.add("bar");
        generator.add("baz");
        generator.add("asdf");

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+foo;bar;baz-", codes.get(0));
        Assert.assertEquals("+asdf-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testSplitAgeRestrictedProducts() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .nextCode("****")
                .nextCodeWithCheck("####")
                .finalCode("%%%%")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        Product duplo = project.getProductDatabase().findBySku("49");
        Product krombacherPils = project.getProductDatabase().findBySku("37");

        project.getShoppingCart().add(duplo, 5, ScannableCode.parse(project, "4008400301020"));
        project.getShoppingCart().add(krombacherPils, 2, ScannableCode.parse(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;4008400301020;4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testSplitAgeRestrictedProductsInMultipleCodes() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .nextCode("****")
                .nextCodeWithCheck("####")
                .finalCode("%%%%")
                .maxChars(40)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        Product duplo = project.getProductDatabase().findBySku("49");
        Product krombacherPils = project.getProductDatabase().findBySku("37");

        project.getShoppingCart().add(duplo, 3, ScannableCode.parse(project, "4008400301020"));
        project.getShoppingCart().add(krombacherPils, 3, ScannableCode.parse(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(3, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008400301020;4008287051124;####-", codes.get(1));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(2));
    }

    @Test
    @UiThreadTest
    public void testSplitAgeRestrictedProductsInMultipleCodes2() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .nextCode("****")
                .nextCodeWithCheck("####")
                .finalCode("%%%%")
                .maxChars(33)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        Product duplo = project.getProductDatabase().findBySku("49");
        Product krombacherPils = project.getProductDatabase().findBySku("37");

        project.getShoppingCart().add(duplo, 3, ScannableCode.parse(project, "4008400301020"));
        project.getShoppingCart().add(krombacherPils, 3, ScannableCode.parse(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(6, codes.size());
        Assert.assertEquals("+4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008400301020;####-", codes.get(1));
        Assert.assertEquals("+4008400301020;####-", codes.get(2));
        Assert.assertEquals("+4008287051124;####-", codes.get(3));
        Assert.assertEquals("+4008287051124;####-", codes.get(4));
        Assert.assertEquals("+4008287051124;%%%%-", codes.get(5));
    }

    @Test
    @UiThreadTest
    public void testSplitWithOnlyOneRestrictedProduct() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("+")
                .separator(";")
                .suffix("-")
                .nextCode("****")
                .nextCodeWithCheck("####")
                .finalCode("%%%%")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);
        Product krombacherPils = project.getProductDatabase().findBySku("37");

        project.getShoppingCart().add(krombacherPils, 1, ScannableCode.parse(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+####-", codes.get(0));
        Assert.assertEquals("+4008287051124;%%%%-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testKnauber() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("")
                .separator("\n")
                .suffix("")
                .nextCode("")
                .nextCodeWithCheck("")
                .finalCode("2030801009061")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        generator.add("voucher1");
        generator.add("voucher2");

        Product duplo = project.getProductDatabase().findBySku("49");
        project.getShoppingCart().add(duplo, 3, ScannableCode.parse(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        project.getShoppingCart().add(heinz, 2, ScannableCode.parse(project, "8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("voucher1\nvoucher2\n4008400301020\n4008400301020\n4008400301020\n8715700421698\n8715700421698\n2030801009061", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testEdeka() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("XE")
                .separator("XE")
                .suffix("XZ")
                .nextCode("0000000001234")
                .nextCodeWithCheck("0000000012345")
                .finalCode("0000000013444")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("48");
        project.getShoppingCart().add(duplo, 3, ScannableCode.parse(project, "42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("XE0000042276630XE0000042276630XE0000042276630XE0000000013444XZ", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testEdekaWithOverflow() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("XE")
                .separator("XE")
                .suffix("XZ")
                .nextCode("0000000001234")
                .nextCodeWithCheck("0000000012345")
                .finalCode("0000000013444")
                .maxChars(677)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("48");
        project.getShoppingCart().add(duplo, 45, ScannableCode.parse(project, "42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 44) + "XE0000000001234XZ", codes.get(0));
        Assert.assertEquals("XE0000042276630XE0000000013444XZ", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testEdekaWithRestrictedProduct() throws IOException, Snabble.SnabbleException {
        withDb("demoDb_1_11.sqlite3");

        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("XE")
                .separator("XE")
                .suffix("XZ")
                .nextCode("0000000001234")
                .nextCodeWithCheck("0000000012345")
                .finalCode("0000000013444")
                .maxChars(675)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("48");
        project.getShoppingCart().add(duplo, 3, ScannableCode.parse(project, "42276630"));

        Product krombacherPils = project.getProductDatabase().findBySku("37");
        project.getShoppingCart().add(krombacherPils, 1, ScannableCode.parse(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 3) + "XE0000000012345XZ", codes.get(0));
        Assert.assertEquals("XE4008287051124XE0000000013444XZ", codes.get(1));
    }
}

