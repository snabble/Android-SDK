package io.snabble.sdk;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void testSplitAgeRestrictedProducts() {
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

        project.getShoppingCart().add(duplo, 5, ScannableCode.parseDefault("4008400301020"));
        project.getShoppingCart().add(krombacherPils, 2, ScannableCode.parseDefault("4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;4008400301020;4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testSplitAgeRestrictedProductsInMultipleCodes() {
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

        project.getShoppingCart().add(duplo, 3, ScannableCode.parseDefault("4008400301020"));
        project.getShoppingCart().add(krombacherPils, 3, ScannableCode.parseDefault("4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(3, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008400301020;4008287051124;####-", codes.get(1));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(2));
    }

    @Test
    @UiThreadTest
    public void testSplitAgeRestrictedProductsInMultipleCodes2() {
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

        project.getShoppingCart().add(duplo, 3, ScannableCode.parseDefault("4008400301020"));
        project.getShoppingCart().add(krombacherPils, 3, ScannableCode.parseDefault("4008287051124"));

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
    public void testSplitWithOnlyOneRestrictedProduct() {
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

        project.getShoppingCart().add(krombacherPils, 1, ScannableCode.parseDefault("4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+####-", codes.get(0));
        Assert.assertEquals("+4008287051124;%%%%-", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testKnauber() {
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
        project.getShoppingCart().add(duplo, 3, ScannableCode.parseDefault("4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        project.getShoppingCart().add(heinz, 2, ScannableCode.parseDefault("8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("voucher1\nvoucher2\n8715700421698\n8715700421698\n4008400301020\n4008400301020\n4008400301020\n2030801009061", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testEdeka() {
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
        project.getShoppingCart().add(duplo, 3, ScannableCode.parseDefault("42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("XE0000042276630XE0000042276630XE0000042276630XE0000000013444XZ", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testEdekaWithOverflow() {
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
        project.getShoppingCart().add(duplo, 45, ScannableCode.parseDefault("42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 44) + "XE0000000001234XZ", codes.get(0));
        Assert.assertEquals("XE0000042276630XE0000000013444XZ", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testEdekaWithRestrictedProduct() {
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
        project.getShoppingCart().add(duplo, 3, ScannableCode.parseDefault("42276630"));

        Product krombacherPils = project.getProductDatabase().findBySku("37");
        project.getShoppingCart().add(krombacherPils, 1, ScannableCode.parseDefault("4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 3) + "XE0000000012345XZ", codes.get(0));
        Assert.assertEquals("XE4008287051124XE0000000013444XZ", codes.get(1));
    }

    @Test
    @UiThreadTest
    public void testExpensiveItemsSortedToBottom() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("")
                .separator(";")
                .suffix("")
                .maxCodes(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        project.getShoppingCart().add(duplo, 1, ScannableCode.parseDefault("4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        project.getShoppingCart().add(heinz, 1, ScannableCode.parseDefault("8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("8715700421698;4008400301020", codes.get(0));
    }

    @Test
    @UiThreadTest
    public void testCSVFormat() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder()
                .prefix("snabble;\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(100)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        project.getShoppingCart().add(duplo, 7, ScannableCode.parseDefault("4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        project.getShoppingCart().add(heinz, 1000, ScannableCode.parseDefault("8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;\n1000;8715700421698\n7;4008400301020", codes.get(0));
    }

}

