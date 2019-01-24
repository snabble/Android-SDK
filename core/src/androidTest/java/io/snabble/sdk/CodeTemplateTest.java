package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.codes.templates.CodeTemplate;

public class CodeTemplateTest {
    @Test
    public void testTemplateParser() {
        // valid templates
        newCodeTemplate("96{code:ean13}{embed:6}{price:5}{_}", true);
        newCodeTemplate("2{code:5}{i}{embed:5}{_}", true);
        newCodeTemplate("01{code:ean14}", true);
        newCodeTemplate("97{code:ean13}{price:6}{_}", true);
        newCodeTemplate("96{code:7}{price:5}", true);
        newCodeTemplate("96{_:5}{code:ean13}{_:3}", true);

        newCodeTemplate("96{_:5}{code:ean13}bla{_:3}", true);

        newCodeTemplate("{code:*}", true);
        newCodeTemplate("{*}", true);
        newCodeTemplate("96{code:*}", true);
        newCodeTemplate("96{*}", true);

        newCodeTemplate("123{_:5}", true);

        // invalid templates
        newCodeTemplate("96{prize:5}", false);
        newCodeTemplate("{prize:5}", false);
        newCodeTemplate("96{code{ean13}{embed:6}{price:5}{_}", false);
        newCodeTemplate("96{price:5", false);
        newCodeTemplate("96{price:5}}", false);
        newCodeTemplate("96{}", false);
        newCodeTemplate("96{}{}", false);
        newCodeTemplate("2{code:5}i}{embed:5}{_}", false);
        newCodeTemplate("01{code:ean14}{code:4}", false);
        newCodeTemplate("{code:abc}", false);
        newCodeTemplate("96{i}{price:5}", false);
        newCodeTemplate("96{i}{embed:6}", false);
        newCodeTemplate("96price:5}", false);
        newCodeTemplate("", false);
        newCodeTemplate("{embed:-1}", false);
        newCodeTemplate("{code:-1}", false);
        newCodeTemplate("{embed:0}", false);
        newCodeTemplate("{code:0}", false);
        newCodeTemplate("{code:}", false);
        newCodeTemplate("{embed:}", false);
        newCodeTemplate("{price:}", false);
    }

    @Test
    public void testEncoding() {
        ScannableCode code = newCodeTemplate("97{code:ean13}{embed:6}{_}").match("9743115013222840001009");
        Assert.assertEquals("4311501322284", code.getLookupCode());
        Assert.assertEquals(100, code.getEmbeddedData());
    }

    @Test
    public void testTemplateMatcher() {
        Assert.assertNotNull(newCodeTemplate("96{code:ean13}{embed:6}{price:5}{_}").match("960000000000000111111222223"));

        // valid matches
        Assert.assertNotNull(newCodeTemplate("{code:ean13}").match("0000000000000"));
        Assert.assertNotNull(newCodeTemplate("{code:ean13}").match("0000000000017"));
        Assert.assertNotNull(newCodeTemplate("{code:ean13}").match("2957783000742"));
        Assert.assertNotNull(newCodeTemplate("{code:ean13}").match("4029764001807"));
        Assert.assertNotNull(newCodeTemplate("{code:ean8}").match("87654325"));
        Assert.assertNotNull(newCodeTemplate("{code:8}").match("87654325"));
        Assert.assertNotNull(newCodeTemplate("{code:8}").match("87654320"));
        Assert.assertNotNull(newCodeTemplate("{code:ean14}").match("18594001694690"));
        Assert.assertNotNull(newCodeTemplate("{code:ean14}").match("28000017120605"));
        Assert.assertNotNull(newCodeTemplate("96{code:ean13}{embed:6}{price:5}{_}").match("960000000000000111111222223"));
        Assert.assertNotNull(newCodeTemplate("123{_:5}").match("12345678"));

        Assert.assertEquals("123", newCodeTemplate("123{_:5}{code:3}").match("12345678123").getLookupCode());

        // invalid matches
        Assert.assertNull(newCodeTemplate("{code:ean13}").match("0000000000001"));
        Assert.assertNull(newCodeTemplate("{code:ean13}").match("000000000000"));
        Assert.assertNull(newCodeTemplate("{code:ean13}").match("4029764001800"));
        Assert.assertNull(newCodeTemplate("{code:ean8}").match("87654320"));
        Assert.assertNull(newCodeTemplate("96{code:ean13}{embed:6}{price:5}{_}").match("970000000000000111111222223"));
        Assert.assertNull(newCodeTemplate("96{code:ean13}{embed:6}{price:5}{_}").match("96000000000000011111122222"));
        Assert.assertNull(newCodeTemplate("123{_:5}").match("55545678"));
    }

    @Test
    public void testWildcardMatcher() {
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("0000000000000"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("0000000000017"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("2957783000742"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("4029764001807"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("ASDF"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("SKDFHSIDUFS"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("!73487#48tnq72**"));
        Assert.assertNotNull(newCodeTemplate("{code:*}").match("******3732ha"));

        Assert.assertNotNull(newCodeTemplate("{*}").match("0000000000000"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("0000000000017"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("2957783000742"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("4029764001807"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("ASDF"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("SKDFHSIDUFS"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("!73487#48tnq72**"));
        Assert.assertNotNull(newCodeTemplate("{*}").match("******3732ha"));

        Assert.assertNotNull(newCodeTemplate("1234{code:*}").match("1234******3732ha"));
        Assert.assertNotNull(newCodeTemplate("12345{code:*}").match("12345******3732ha"));
        Assert.assertNotNull(newCodeTemplate("1234{*}").match("1234******3732ha"));
        Assert.assertNotNull(newCodeTemplate("12345{*}").match("12345******3732ha"));
        Assert.assertNull(newCodeTemplate("12345{*}").match("bla******3732ha"));
        Assert.assertNull(newCodeTemplate("12345{code:*}").match("bla******3732ha"));
    }
    
    @Test
    public void testTemplateInternalChecksum() {
        Assert.assertNotNull(newCodeTemplate("295778{i}{embed:5}{_}").match("2957783000742"));
        Assert.assertNull(newCodeTemplate("295778{i}{embed:5}{_}").match("2957784000742"));

        Assert.assertNotNull(newCodeTemplate("2{code:5}{i}{embed:5}{_}").match("2957783000742"));
        Assert.assertNull(newCodeTemplate("2{code:5}{i}{embed:5}{_}").match("2957784000742"));
    }

    private CodeTemplate newCodeTemplate(String pattern) {
        return newCodeTemplate(pattern, true);
    }
    
    private CodeTemplate newCodeTemplate(String pattern, boolean expect) {
        try {
            CodeTemplate codeTemplate = new CodeTemplate("", pattern);

            if (!expect) {
                Assert.fail("CodeTemplate " + codeTemplate.getPattern() + " was created, but should have thrown a Exception");
            }
            return codeTemplate;
        } catch (IllegalArgumentException e) {
            if (expect) {
                Assert.fail(e.getMessage());
            }
        }

        return null;
    }
}