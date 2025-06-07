# Think Tag Filtering Fix

## Issue Description
AI assistant responses containing `<think>` tags with internal reasoning content were causing VARCHAR(10000) limit to be exceeded when saving to the `chat_messages` table in the database.

## Root Cause
The LLM was generating responses that included `<think>` tags containing internal reasoning that wasn't meant to be saved to the database. These internal reasoning blocks were sometimes very large, causing the total response to exceed the database column limit.

## Solution Implementation

### 1. ContentFilterUtil Class
Created `src/main/java/com/steffenhebestreit/ai_research/Util/ContentFilterUtil.java` with:

- **THINK_TAG_PATTERN**: Regex pattern to match `<think>.*?</think>` blocks (case-insensitive, DOTALL mode)
- **MAX_CONTENT_LENGTH**: Constant (10000) matching database VARCHAR constraint
- **filterForDatabase()**: Main filtering method that removes think tags and validates length
- **containsThinkTags()**: Helper method to check if content contains think tags
- **getMaxContentLength()**: Getter for the maximum allowed content length

### 2. Applied Filtering to Streaming Endpoints

#### TaskController.chatStream()
- **Location**: `doOnComplete()` callback in streaming response handler
- **Change**: Replace `fullResponse` with `ContentFilterUtil.filterForDatabase(fullResponse)` before saving to chat history
- **Impact**: Prevents think tag content from being saved to database

#### MultimodalController.chatStreamMultimodal()
- **Location**: `doOnComplete()` callback in streaming response handler  
- **Change**: Applied same filtering logic as TaskController
- **Impact**: Ensures multimodal responses also have think tags filtered

### 3. Import Statements Added
Added `import com.steffenhebestreit.ai_research.Util.ContentFilterUtil;` to:
- TaskController.java
- MultimodalController.java

## Technical Details

### Regex Pattern
```java
private static final Pattern THINK_TAG_PATTERN = Pattern.compile(
    "<think>.*?</think>", 
    Pattern.DOTALL | Pattern.CASE_INSENSITIVE
);
```

### Filtering Logic
1. Remove all `<think>.*?</think>` blocks from content
2. Trim whitespace from the filtered result
3. Validate that resulting content length ≤ 10000 characters
4. Throw exception if content still exceeds limit after filtering

### Database Constraint
- Table: `chat_messages`
- Column: `content`
- Type: `VARCHAR(10000)`
- Constraint: Maximum 10000 characters

## Testing

### Created Test Suite
`src/test/java/com/steffenhebestreit/ai_research/Util/ContentFilterUtilTest.java` with test cases for:
- Basic think tag removal
- Nested think tags
- Case insensitive filtering
- Multiple think blocks
- Content without think tags
- Length validation
- Edge cases (empty content, whitespace)

### Verification
- ✅ All existing controller tests pass
- ✅ Compilation successful with no errors
- ✅ ContentFilterUtil tests pass
- ✅ No breaking changes to existing functionality

## Architecture Notes

### Controllers Not Modified
**ChatController.streamMessage()**: This endpoint was intentionally NOT modified because:
- It doesn't automatically save responses to the database
- It expects the frontend to handle response persistence
- Different architectural pattern from TaskController and MultimodalController

### Response Streaming Preserved
- The filtering only affects what gets saved to the database
- Users still see the complete streamed response in real-time
- Think tag content is visible during streaming but filtered from persistence

## Benefits

1. **Database Integrity**: Prevents VARCHAR overflow errors
2. **Content Cleanliness**: Removes internal AI reasoning from stored conversations
3. **Performance**: Reduces database storage requirements
4. **User Experience**: Users see full responses during streaming, but storage is optimized
5. **Backward Compatibility**: No changes to existing API contracts

## Future Considerations

1. **Configuration**: Could make the filtering pattern configurable
2. **Logging**: Consider logging when think tags are filtered for monitoring
3. **Alternative Storage**: For debugging, could store full responses in separate table
4. **Pattern Extension**: Could extend to filter other internal AI tags if needed

## Files Modified

1. **NEW**: `src/main/java/com/steffenhebestreit/ai_research/Util/ContentFilterUtil.java`
2. **MODIFIED**: `src/main/java/com/steffenhebestreit/ai_research/Controller/TaskController.java`
3. **MODIFIED**: `src/main/java/com/steffenhebestreit/ai_research/Controller/MultimodalController.java`
4. **NEW**: `src/test/java/com/steffenhebestreit/ai_research/Util/ContentFilterUtilTest.java`
5. **NEW**: `docs/think-tag-filtering-fix.md` (this document)
