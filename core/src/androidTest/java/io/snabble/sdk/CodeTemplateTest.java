package io.snabble.sdk;

import org.junit.Assert;
import org.junit.Test;

import io.snabble.sdk.codes.templates.CodeTemplate;

public class CodeTemplateTest {
    @Test
    public void testTemplateParser() {
        Assert.assertNull(CodeTemplate.parse("96{i}{price:5}"));

        // valid templates
        Assert.assertNotNull(CodeTemplate.parse("96{code:ean13}{weight:6}{price:5}{_}"));
        Assert.assertNotNull(CodeTemplate.parse("2{code:5}{i}{weight:5}{_}"));
        Assert.assertNotNull(CodeTemplate.parse("01{code:ean14}"));
        Assert.assertNotNull(CodeTemplate.parse("97{code:ean13}{price:6}{_}"));
        Assert.assertNotNull(CodeTemplate.parse("96{code:7}{price:5}"));
        Assert.assertNotNull(CodeTemplate.parse("96{_:5}{code:ean13}{_:3}"));

        // new tests
        Assert.assertNotNull(CodeTemplate.parse("96{_:5}{code:ean13}bla{_:3}"));

        // invalid templates
        Assert.assertNull(CodeTemplate.parse("96{prize:5}"));
        Assert.assertNull(CodeTemplate.parse("{prize:5}"));
        Assert.assertNull(CodeTemplate.parse("96{code{ean13}{weight:6}{price:5}{_}"));
        Assert.assertNull(CodeTemplate.parse("96{price:5"));
        Assert.assertNull(CodeTemplate.parse("96{price:5}}"));
        Assert.assertNull(CodeTemplate.parse("96{}"));
        Assert.assertNull(CodeTemplate.parse("96{}{}"));
        Assert.assertNull(CodeTemplate.parse("2{code:5}i}{weight:5}{_}"));
        Assert.assertNull(CodeTemplate.parse("01{code:ean14}{code:4}"));
        Assert.assertNull(CodeTemplate.parse("{code:abc}"));
        Assert.assertNull(CodeTemplate.parse("96{i}{price:5}"));
        Assert.assertNull(CodeTemplate.parse("96{i}{weight:6}"));
        Assert.assertNull(CodeTemplate.parse("96price:5}"));
        Assert.assertNull(CodeTemplate.parse(""));
        Assert.assertNull(CodeTemplate.parse("{weight:-1}"));
        Assert.assertNull(CodeTemplate.parse("{code:-1}"));
    }

    @Test
    public void testTemplateMatcher() {
        Assert.assertNotNull(CodeTemplate.parse("96{code:ean13}{weight:6}{price:5}{_}").match("960000000000000111111222223"));

        // valid matches
        Assert.assertNotNull(CodeTemplate.parse("{code:ean13}").match("0000000000000"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean13}").match("0000000000017"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean13}").match("2957783000742"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean13}").match("4029764001807"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean8}").match("87654325"));
        Assert.assertNotNull(CodeTemplate.parse("{code:8}").match("87654325"));
        Assert.assertNotNull(CodeTemplate.parse("{code:8}").match("87654320"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean14}").match("18594001694690"));
        Assert.assertNotNull(CodeTemplate.parse("{code:ean14}").match("28000017120605"));
        Assert.assertNotNull(CodeTemplate.parse("96{code:ean13}{weight:6}{price:5}{_}").match("960000000000000111111222223"));

        // invalid matches
        Assert.assertNull(CodeTemplate.parse("{code:ean13}").match("0000000000001"));
        Assert.assertNull(CodeTemplate.parse("{code:ean13}").match("000000000000"));
        Assert.assertNull(CodeTemplate.parse("{code:ean13}").match("4029764001800"));
        Assert.assertNull(CodeTemplate.parse("{code:ean8}").match("87654320"));
        Assert.assertNull(CodeTemplate.parse("96{code:ean13}{weight:6}{price:5}{_}").match("970000000000000111111222223"));
        Assert.assertNull(CodeTemplate.parse("96{code:ean13}{weight:6}{price:5}{_}").match("96000000000000011111122222"));
    }
    
    @Test
    public void testTemplateInternalChecksum() {
        Assert.assertNotNull(CodeTemplate.parse("295778{i}{weight:5}{_}").match("2957783000742"));
        Assert.assertNull(CodeTemplate.parse("295778{i}{weight:5}{_}").match("2957784000742"));

        Assert.assertNotNull(CodeTemplate.parse("2{code:5}{i}{weight:5}{_}").match("2957783000742"));
        Assert.assertNull(CodeTemplate.parse("2{code:5}{i}{weight:5}{_}").match("2957784000742"));
    }
}