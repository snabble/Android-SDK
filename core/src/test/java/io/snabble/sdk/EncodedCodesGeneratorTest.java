package io.snabble.sdk;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.coupons.Coupon;
import io.snabble.sdk.coupons.CouponCode;
import io.snabble.sdk.coupons.CouponType;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.shoppingcart.ShoppingCart;

@RunWith(RobolectricTestRunner.class)
public class EncodedCodesGeneratorTest extends SnabbleSdkTest {
    @Test
    public void testGeneration() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
    public void testSplitAtMaxChars() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
    public void testSplitAtMaxCodes() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
    public void testSplitAgeRestrictedProducts() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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

        addToCart(duplo, 5, ScannedCode.parseDefault(project, "4008400301020"));
        addToCart(krombacherPils, 2, ScannedCode.parseDefault(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;4008400301020;4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(1));
    }

    @Test
    public void testSplitAgeRestrictedProductsInMultipleCodes() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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

        addToCart(duplo, 3, ScannedCode.parseDefault(project, "4008400301020"));
        addToCart(krombacherPils, 3, ScannedCode.parseDefault(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(3, codes.size());
        Assert.assertEquals("+4008400301020;4008400301020;####-", codes.get(0));
        Assert.assertEquals("+4008400301020;4008287051124;####-", codes.get(1));
        Assert.assertEquals("+4008287051124;4008287051124;%%%%-", codes.get(2));
    }

    @Test
    public void testSplitAgeRestrictedProductsInMultipleCodes2() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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

        addToCart(duplo, 3, ScannedCode.parseDefault(project, "4008400301020"));
        addToCart(krombacherPils, 3, ScannedCode.parseDefault(project, "4008287051124"));

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
    public void testSplitWithOnlyOneRestrictedProduct() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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

        addToCart(krombacherPils, 1, ScannedCode.parseDefault(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("+####-", codes.get(0));
        Assert.assertEquals("+4008287051124;%%%%-", codes.get(1));
    }

    @Test
    public void testKnauber() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
        addToCart(duplo, 3, ScannedCode.parseDefault(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        addToCart(heinz, 2, ScannedCode.parseDefault(project, "8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("voucher1\nvoucher2\n8715700421698\n8715700421698\n4008400301020\n4008400301020\n4008400301020\n2030801009061", codes.get(0));
    }

    @Test
    public void testCoupons() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("")
                .separator("\n")
                .suffix("")
                .nextCode("")
                .nextCodeWithCheck("")
                .maxChars(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 1, ScannedCode.parseDefault(project, "4008400301020"));

        ShoppingCart cart = project.getShoppingCart();
        cart.add(cart.newItem(
                new Coupon("asdf", "foo", null, null, CouponType.PRINTED, Collections.singletonList(new CouponCode("1234", "default")), null, null, null, null, null, null),
                ScannedCode.parseDefault(project, "1234")
        ));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("4008400301020\n1234", codes.get(0));
    }

    @Test
    public void testEdeka() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
        addToCart(duplo, 3, ScannedCode.parseDefault(project, "42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("XE0000042276630XE0000042276630XE0000042276630XE0000000013444XZ", codes.get(0));
    }

    @Test
    public void testEdekaWithOverflow() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
        addToCart(duplo, 45, ScannedCode.parseDefault(project, "42276630"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 44) + "XE0000000001234XZ", codes.get(0));
        Assert.assertEquals("XE0000042276630XE0000000013444XZ", codes.get(1));
    }

    @Test
    public void testEdekaWithRestrictedProduct() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
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
        addToCart(duplo, 3, ScannedCode.parseDefault(project, "42276630"));

        Product krombacherPils = project.getProductDatabase().findBySku("37");
        addToCart(krombacherPils, 1, ScannedCode.parseDefault(project, "4008287051124"));

        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals(StringUtils.repeat("XE0000042276630", 3) + "XE0000000012345XZ", codes.get(0));
        Assert.assertEquals("XE4008287051124XE0000000013444XZ", codes.get(1));
    }

    @Test
    public void testExpensiveItemsSortedToBottom() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("")
                .separator(";")
                .suffix("")
                .maxCodes(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 1, ScannedCode.parseDefault(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        addToCart(heinz, 1, ScannedCode.parseDefault(project, "8715700421698"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("8715700421698;4008400301020", codes.get(0));
    }

    @Test
    public void testCSVFormat() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("snabble;\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(100)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 7, ScannedCode.parseDefault(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        addToCart(heinz, 1000, ScannedCode.parseDefault(project, "8715700421698"));
        generator.add("asdf123");
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;\n1;asdf123\n1000;8715700421698\n7;4008400301020", codes.get(0));
    }

    @Test
    public void testCSVv2Format() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("snabble;{qrCodeIndex};{qrCodeCount}\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(2)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 7, ScannedCode.parseDefault(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        addToCart(heinz, 1000, ScannedCode.parseDefault(project, "8715700421698"));
        generator.add("asdf123");
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("snabble;1;2\n1;asdf123\n1000;8715700421698", codes.get(0));
        Assert.assertEquals("snabble;2;2\n7;4008400301020", codes.get(1));
    }

    @Test
    public void testCSVv2FormatWithCheckoutId() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("snabble;{qrCodeIndex};{qrCodeCount};{checkoutId}\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(2)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 7, ScannedCode.parseDefault(project, "4008400301020"));

        Product heinz = project.getProductDatabase().findBySku("42");
        addToCart(heinz, 1000, ScannedCode.parseDefault(project, "8715700421698"));
        generator.add("asdf123");
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate("TEST_CHECKOUT_ID_1234");
        Assert.assertEquals(2, codes.size());
        Assert.assertEquals("snabble;1;2;TEST_CHECKOUT_ID_1234\n1;asdf123\n1000;8715700421698", codes.get(0));
        Assert.assertEquals("snabble;2;2;TEST_CHECKOUT_ID_1234\n7;4008400301020", codes.get(1));
    }

    @Test
    public void testFinalCodeWithManualDiscounts() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("snabble;{qrCodeIndex};{qrCodeCount}\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(2)
                .manualDiscountFinalCode("MANUAL_DISCOUNT")
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 7, ScannedCode.parseDefault(project, "4008400301020"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;1;1\n7;4008400301020", codes.get(0));

        ShoppingCart cart = project.getShoppingCart();
        cart.get(0).setManualCouponApplied(true);
        generator.add(project.getShoppingCart());

        codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;1;1\n7;4008400301020\n1;MANUAL_DISCOUNT", codes.get(0));
    }

    @Test
    public void testFinalCodeWithManualDiscountsAndFinalCode() {
        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("snabble;{qrCodeIndex};{qrCodeCount}\n")
                .separator("\n")
                .suffix("")
                .repeatCodes(false)
                .countSeparator(";")
                .maxCodes(100)
                .finalCode("FINAL_CODE")
                .manualDiscountFinalCode("MANUAL_DISCOUNT")
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product duplo = project.getProductDatabase().findBySku("49");
        addToCart(duplo, 7, ScannedCode.parseDefault(project, "4008400301020"));
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;1;1\n7;4008400301020\n1;FINAL_CODE", codes.get(0));

        ShoppingCart cart = project.getShoppingCart();
        cart.get(0).setManualCouponApplied(true);
        generator.add(project.getShoppingCart());

        codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("snabble;1;1\n7;4008400301020\n1;FINAL_CODE\n1;MANUAL_DISCOUNT", codes.get(0));
    }

    @Test
    public void testTransmissionTemplates() throws IOException, Snabble.SnabbleException {
        String[] sql = loadSql("transmission_template").split("\n");
        withDb("test_1_25.sqlite3", false, Arrays.asList(sql));

        EncodedCodesOptions options = new EncodedCodesOptions.Builder(project)
                .prefix("")
                .separator(";")
                .suffix("")
                .maxCodes(1000)
                .build();

        EncodedCodesGenerator generator = new EncodedCodesGenerator(options);

        Product braeburn = project.getProductDatabase().findBySku("34-tt");
        List<ScannedCode> scannedCodes = ScannedCode.parse(project, "2123451001002");
        ScannedCode scannedCode = null;
        for (ScannedCode code : scannedCodes) {
            if (code.getTemplateName().equals("ean13_instore")) {
                scannedCode = code;
                break;
            }
        }

        addToCart(braeburn, 1, scannedCode);
        generator.add(project.getShoppingCart());

        ArrayList<String> codes = generator.generate();
        Assert.assertEquals(1, codes.size());
        Assert.assertEquals("TEST_00100_12345", codes.get(0));
    }

    private void addToCart(Product product, int quantity, ScannedCode scannedCode) {
        ShoppingCart cart = project.getShoppingCart();
        ShoppingCart.Item item = cart.newItem(product, scannedCode);
        item.setQuantity(quantity);
        cart.add(item);
    }
}
