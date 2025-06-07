package com.steffenhebestreit.ai_research.Util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentFilterUtil class.
 */
class ContentFilterUtilTest {    @Test
    void testFilterForDatabase_RemovesThinkTags() {
        String content = "This is a normal response. <think>This is internal reasoning that should be removed.</think> This should remain.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("This is a normal response. This should remain.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesMultipleThinkTags() {
        String content = "Start <think>first thought</think> middle <think>second thought</think> end.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Start middle end.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesThinkTagsWithNewlines() {
        String content = "Normal text. <think>\nMultiline reasoning\nwith breaks\n</think> More normal text.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Normal text. More normal text.", filtered);
    }    @Test
    void testFilterForDatabase_CaseInsensitive() {
        String content = "Text <THINK>uppercase</THINK> and <Think>mixed case</Think> content.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Text and content.", filtered);
    }

    @Test
    void testFilterForDatabase_NoThinkTags() {
        String content = "This is a normal response without any think tags.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals(content, filtered);
    }

    @Test
    void testFilterForDatabase_TruncatesLongContent() {
        StringBuilder longContent = new StringBuilder();
        // Generate content longer than MAX_CONTENT_LENGTH
        for (int i = 0; i < ContentFilterUtil.getMaxContentLength() * 1.5; i++) {
            longContent.append("a");
        }
        String filtered = ContentFilterUtil.filterForDatabase(longContent.toString());
        // Expect truncation to MAX_CONTENT_LENGTH
        assertEquals(ContentFilterUtil.getMaxContentLength(), filtered.length());
    }

    @Test
    void testFilterForDatabase_HandlesNullInput() {
        String filtered = ContentFilterUtil.filterForDatabase(null);
        assertEquals("", filtered);
    }

    @Test
    void testFilterForDatabase_HandlesEmptyInput() {
        String filtered = ContentFilterUtil.filterForDatabase("");
        assertEquals("", filtered);
    }

    @Test
    void testContainsThinkTags_ReturnsTrueWhenPresent() {
        String content = "Normal text <think>reasoning</think> more text.";
        assertTrue(ContentFilterUtil.containsThinkTags(content));
    }

    @Test
    void testContainsThinkTags_ReturnsFalseWhenAbsent() {
        String content = "Normal text without any special tags.";
        assertFalse(ContentFilterUtil.containsThinkTags(content));
    }

    @Test
    void testGetMaxContentLength() {
        // Should match the configured database VARCHAR limit
        assertEquals(30000, ContentFilterUtil.getMaxContentLength());
    }    @Test
    void testFilterForDatabase_RemovesToolCodeTags() {
        String content = "Let me analyze this. <tool_code>This is a tool invocation that should be removed.</tool_code> Here's what I found.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me analyze this. Here's what I found.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesMultipleToolCodeTags() {
        String content = "First <tool_code>crawl request</tool_code> and then <tool_code>analyze request</tool_code> results.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("First and then results.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesToolCodeTagsWithNewlines() {
        String content = "Starting analysis. <tool_code>\n{\n  \"url\": \"example.com\"\n}\n</tool_code> Results ready.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Starting analysis. Results ready.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesBothThinkAndToolCodeTags() {
        String content = "Let me think. <thinking>Should I use a tool?</thinking> Yes! <tool_code>Use tool now</tool_code> Done!";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me think. Yes! Done!", filtered);
    }@Test
    void testFilterForDatabase_RemovesToolExecutionStatusMessages() {
        String content = "Let me check the time. [Calling tool: dateTime] [Executing tools...] The current time in Berlin is 2:00 PM.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me check the time. The current time in Berlin is 2:00 PM.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesMultipleExecutionMessages() {
        String content = "[Calling tool: generateSitemap] [Executing tools...] [Executing: smartCrawl] [Tool execution failed: null result] Let me try another approach.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me try another approach.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesComplexToolMessages() {
        String content = "Let me try. [Calling tool: searchWeb] Website info: [Tool error: Connection timeout] [Continuing conversation: with 0 successful and 1 failed tool results] I'll use another source.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me try. Website info: I'll use another source.", filtered);
    }@Test
    void testFilterForDatabase_RemovesAllTypesOfContent() {
        String content = "<thinking>Should I search the web?</thinking> Yes, let's do that. [Calling tool: searchWeb] <tool_code>{'query': 'example.com'}</tool_code> [Executing tools...] Here are the results.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Yes, let's do that. Here are the results.", filtered);
    }@Test
    void testFilterForDatabase_RemovesVariedToolExecutionMessages() {
        String content = "Let me check your website. [Calling tool: urlAnalyzer] [Tool result: Site has 10 pages] The site looks good.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me check your website. The site looks good.", filtered);
    }    
    
    @Test
    void testFilterForDatabase_RemovesBackgroundToolExecutionMessages() {
        String content = "Let me analyze your data. [Tool execution continues in background beyond maximum wait time] The analysis might take some time.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me analyze your data. The analysis might take some time.", filtered);
    }@Test
    void testFilterForDatabase_RemovesStepNumbers() {
        String content = "[Step 1: Analyzing request] I'll analyze your code. [Step 2: Writing solution] Here's the solution.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("I'll analyze your code. Here's the solution.", filtered);
    }    @Test
    void testFilterForDatabase_RemovesUsingToolMessages() {
        String content = "[Using tool: webCrawler] [Tool execution: Started] [Tool execution: Completed] Found 5 broken links on your site.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Found 5 broken links on your site.", filtered);
    }

    @Test
    void testContainsThinkTags_ChecksAllPatterns() {
        // Test think tags
        assertTrue(ContentFilterUtil.containsThinkTags("<think>Some thinking</think>"));
        
        // Test tool code tags
        assertTrue(ContentFilterUtil.containsThinkTags("<tool_code>Some code</tool_code>"));
        
        // Test tool execution status
        assertTrue(ContentFilterUtil.containsThinkTags("[Calling tool: search]"));
        
        // Test with no patterns
        assertFalse(ContentFilterUtil.containsThinkTags("Normal text without any special patterns"));
    }@Test
    void testFilterForDatabase_RemovesAdditionalToolExecutionPatterns() {
        String content = "[Processing request] First I'll [Executing: parseRequest] analyze this. [Tool thinking: maybe I should] [Task started: Analysis] [Task complete: Found solution] [Tool output: Solution found] Here's the result.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("First I'll analyze this. Here's the result.", filtered);
    }@Test
    void testFilterForDatabase_RemovesToolCompletedSuccessfully() {
        String content = "Let me fetch that data for you. [Tool completed successfully] Here are the results of your query.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Let me fetch that data for you. Here are the results of your query.", filtered);
    }

    @Test
    void testFilterForDatabase_RemovesToolCompletedWithoutDescription() {
        String content = "Running the analysis now. [Tool completed] Your data shows the following trends.";
        String filtered = ContentFilterUtil.filterForDatabase(content);
        assertEquals("Running the analysis now. Your data shows the following trends.", filtered);
    }
}
