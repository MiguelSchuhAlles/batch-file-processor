#!/bin/bash

LOCAL_DIR="./test_files"
BUCKET_NAME="varejosimples-SOMECOMPANY"
MAX_RETRIES=3
UPLOAD_FAILED=0

# Today in YYYYMMDD format
TODAY=$(date +%Y%m%d)

# Regex pattern for date prefix (8 digits YYYYMMDD + hyphen)
DATE_PREFIX_REGEX='^[0-9]{8}-'

# Loop over each file in the local directory
for FILE_PATH in "$LOCAL_DIR"/*; do
    if [ -f "$FILE_PATH" ]; then  # Ensure it's a file
        BASENAME=$(basename "$FILE_PATH")

        # Check if filename already starts with a date before prepending it
        if [[ "$BASENAME" =~ $DATE_PREFIX_REGEX ]]; then
            NEW_FILENAME="$BASENAME"
        else
            NEW_FILENAME="${TODAY}-${BASENAME}"
        fi

        echo "Uploading $FILE_PATH as s3://$BUCKET_NAME/pending_processing/$NEW_FILENAME"

        ATTEMPT=1
        SUCCESS=0

        while [ $ATTEMPT -le $MAX_RETRIES ]; do
            OUTPUT=$(aws s3 cp "$FILE_PATH" "s3://$BUCKET_NAME/pending_processing/$NEW_FILENAME" 2>&1)
            if [ $? -eq 0 ]; then
                echo "✅ Uploaded successfully on attempt $ATTEMPT: $NEW_FILENAME"
                SUCCESS=1
                break
            else
                echo "⚠️ Upload attempt $ATTEMPT failed for $NEW_FILENAME."
                echo "    Reason: $OUTPUT"
                ATTEMPT=$((ATTEMPT + 1))
                sleep 2  # Wait before retry
            fi
        done

        if [ $SUCCESS -eq 0 ]; then
            echo "❌ Failed to upload after $MAX_RETRIES attempts: $FILE_PATH"
            UPLOAD_FAILED=1
        fi
    fi
done

# Exit code based on whether any upload failed
if [ $UPLOAD_FAILED -eq 1 ]; then
    echo "Some files failed to upload."
    exit 1
else
    echo "All files uploaded successfully."
    exit 0
fi