package io.snabble.sdk.utils

import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import io.snabble.sdk.ui.parseLinksInto
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class LinkParserTest {
    private data class SpanData(
        val start: Int,
        val end: Int,
        val what: Any,
    )

    private class MockSpannableStringBuilder {
        val builder: SpannableStringBuilder = mock(SpannableStringBuilder::class.java)
        private val data = StringBuilder()
        val output: String get() = data.toString()
        val spans = mutableListOf<SpanData>()

        init {
            whenever(builder.append(any(CharSequence::class.java))).then { answer ->
                data.append(answer.arguments[0])
                builder
            }
            whenever(builder.length).thenAnswer { data.length }
            whenever(builder.setSpan(any(), anyInt(), anyInt(), anyInt())).thenAnswer {
                spans.add(
                    SpanData(
                        start = it.arguments[1] as Int,
                        end = it.arguments[2] as Int,
                        what = it.arguments[0]
                    )
                )
            }
        }
    }

    @Test
    fun markdown() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("hallo [linked](target) world!", mock.builder)
        assertEquals("hallo linked world!", mock.output)
        assertEquals("Expect one detected link", 1, mock.spans.size)
        assertEquals("Unexpected link start", mock.spans.first().start, 6)
        assertEquals("Unexpected link end", mock.spans.first().end, 12)
        assertEquals("Unexpected span", mock.spans.first().what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected link target", (mock.spans.first().what as URLSpan).url, "target")
    }

    @Test
    fun htmlDoubleQuote() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("hallo <a href=\"target\">linked</a> world!", mock.builder)
        assertEquals("hallo linked world!", mock.output)
        assertEquals("Expect one detected link", 1, mock.spans.size)
        assertEquals("Unexpected link start", mock.spans.first().start, 6)
        assertEquals("Unexpected link end", mock.spans.first().end, 12)
        assertEquals("Unexpected span", mock.spans.first().what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected link target", (mock.spans.first().what as URLSpan).url, "target")
    }

    @Test
    fun htmlSingleQuote() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("hallo <a href='target'>linked</a> world!", mock.builder)
        assertEquals("hallo linked world!", mock.output)
        assertEquals("Expect one detected link", 1, mock.spans.size)
        assertEquals("Unexpected link start", mock.spans.first().start, 6)
        assertEquals("Unexpected link end", mock.spans.first().end, 12)
        assertEquals("Unexpected span", mock.spans.first().what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected link target", (mock.spans.first().what as URLSpan).url, "target")
    }

    @Test
    fun multipleHtmlLinks() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("This <a href='first'>is</a> an <a href=\"second\">extended</a> <b>Test</b>!", mock.builder)
        assertEquals("This is an extended <b>Test</b>!", mock.output)
        assertEquals("Expect two detected links", 2, mock.spans.size)
        assertEquals("Unexpected first link start", mock.spans[0].start, 5)
        assertEquals("Unexpected first link end", mock.spans[0].end, 7)
        assertEquals("Unexpected first span", mock.spans[0].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected first link target", (mock.spans.first().what as URLSpan).url, "first")
        assertEquals("Unexpected second link start", mock.spans[1].start, 11)
        assertEquals("Unexpected second link end", mock.spans[1].end, 19)
        assertEquals("Unexpected second span", mock.spans[1].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected second link target", (mock.spans[1].what as URLSpan).url, "second")
    }

    @Test
    fun multipleMarkdownLinks() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("This [is](first) an [extended](second) *Test*!", mock.builder)
        assertEquals("This is an extended *Test*!", mock.output)
        assertEquals("Expect two detected links", 2, mock.spans.size)
        assertEquals("Unexpected first link start", mock.spans[0].start, 5)
        assertEquals("Unexpected first link end", mock.spans[0].end, 7)
        assertEquals("Unexpected first span", mock.spans[0].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected first link target", (mock.spans.first().what as URLSpan).url, "first")
        assertEquals("Unexpected second link start", mock.spans[1].start, 11)
        assertEquals("Unexpected second link end", mock.spans[1].end, 19)
        assertEquals("Unexpected second span", mock.spans[1].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected second link target", (mock.spans[1].what as URLSpan).url, "second")
    }

    @Test
    fun nestedHtmlFormatting() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("A <b>more <a href='first'>complex</a></b> <A HREF='second'><em>Test</em></a>?", mock.builder)
        assertEquals("A <b>more complex</b> <em>Test</em>?", mock.output)
        assertEquals("Expect two detected links", 2, mock.spans.size)
        assertEquals("Unexpected first link start", mock.spans[0].start, 10)
        assertEquals("Unexpected first link end", mock.spans[0].end, 17)
        assertEquals("Unexpected first span", mock.spans[0].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected first link target", (mock.spans.first().what as URLSpan).url, "first")
        assertEquals("Unexpected second link start", mock.spans[1].start, 22)
        assertEquals("Unexpected second link end", mock.spans[1].end, 35)
        assertEquals("Unexpected second span", mock.spans[1].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected second link target", (mock.spans[1].what as URLSpan).url, "second")
    }

    @Test
    fun nestedMarkdownFormatting() {
        val mock = MockSpannableStringBuilder()
        parseLinksInto("A **more [complex](first)** [[_Test_](second)]?", mock.builder)
        assertEquals("A **more complex** [_Test_]?", mock.output)
        assertEquals("Expect two detected links", 2, mock.spans.size)
        assertEquals("Unexpected first link start", mock.spans[0].start, 9)
        assertEquals("Unexpected first link end", mock.spans[0].end, 16)
        assertEquals("Unexpected first span", mock.spans[0].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected first link target", (mock.spans.first().what as URLSpan).url, "first")
        assertEquals("Unexpected second link start", mock.spans[1].start, 20)
        assertEquals("Unexpected second link end", mock.spans[1].end, 26)
        assertEquals("Unexpected second span", mock.spans[1].what.javaClass, URLSpan::class.java)
        assertEquals("Unexpected second link target", (mock.spans[1].what as URLSpan).url, "second")
    }
}