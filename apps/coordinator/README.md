# FLOW:
### Inputs:
- File watcher
- UI selected file

## Flows - Video file:
### Flow:
    - File watcher
    - Coordinator:
        - Creates process start with:
            - START
            - type: FLOW
            - file: AbssolutePath
    - ReadVideoFileStreams:
        - Reads started event
        - Reads result
        - Produces message with result
    - BaseInfo:
        - Extracts info from filename
        - Extracts info from file media streams
        - Produces title and sanitized
    - pyMetadata:
        - Picks up event
        - Searches with sources using title and sanitized
        - Produces result
    
 ----
    - Extract & Encode
        - Starts