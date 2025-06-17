package com.steffenhebestreit.ai_research.Util;

public class ContentFilterTestExample {
    public static void main(String[] args) {
        System.out.println("Testing ContentFilterUtil with optional linebreaks...\n");
        
        // Test 1: Tool call with surrounding newlines
        String test1 = "Here is some content.\n\n[Tool calls requested by LLM. Executing tools...]\n\nMore content here.";
        System.out.println("Test 1 - Tool call with newlines:");
        System.out.println("Input: " + test1.replace("\n", "\\n"));
        System.out.println("Output: " + ContentFilterUtil.filterForDatabase(test1).replace("\n", "\\n"));
        System.out.println("Expected: Clean removal with proper spacing\n");
        
        // Test 2: Multiple tool calls with various spacing
        String test2 = "Content\n\n\n[Tool execution continues]\n\n\n\nMore content\n[Tool completed successfully]\n\nFinal content.";
        System.out.println("Test 2 - Multiple tool calls with various spacing:");
        System.out.println("Input: " + test2.replace("\n", "\\n"));
        System.out.println("Output: " + ContentFilterUtil.filterForDatabase(test2).replace("\n", "\\n"));
        System.out.println("Expected: Clean removal, normalized spacing\n");
        
        // Test 3: Tool call at beginning and end
        String test3 = "\n[Tool started]\nContent in middle\n[Tool completed successfully]\n";
        System.out.println("Test 3 - Tool calls at beginning and end:");
        System.out.println("Input: " + test3.replace("\n", "\\n"));
        System.out.println("Output: " + ContentFilterUtil.filterForDatabase(test3).replace("\n", "\\n"));
        System.out.println("Expected: Only middle content remains\n");
        
        // Test 4: Only tool calls (should result in empty or minimal content)
        String test4 = "\n\n[Tool calls requested by LLM. Executing tools...]\n\n\n\n";
        System.out.println("Test 4 - Only tool calls:");
        System.out.println("Input: " + test4.replace("\n", "\\n"));
        String output4 = ContentFilterUtil.filterForDatabase(test4);
        System.out.println("Output: '" + output4.replace("\n", "\\n") + "'");
        System.out.println("Length: " + output4.length());
        System.out.println("Expected: Empty or minimal content\n");
        
        // Test 5: Mixed content with think tags and tool calls
        String test5 = "<think>reasoning</think>\n\n[Tool execution]\n\nActual response content.\n\n[Tool completed]\n\n";
        System.out.println("Test 5 - Mixed content:");
        System.out.println("Input: " + test5.replace("\n", "\\n"));
        System.out.println("Output: " + ContentFilterUtil.filterForDatabase(test5).replace("\n", "\\n"));
        System.out.println("Expected: Only 'Actual response content.'\n");
    }
}
