$LocalDir = ".\test_files"
$BucketName = "varejosimples-SOMECOMPANY" # options: "varejosimples-banban", "varejosimples-lojasamerica"
$MaxRetries = 3
$UploadFailed = $false

# Today in YYYYMMDD format
$Today = Get-Date -Format "yyyyMMdd"

# Regex pattern for date prefix (8 digits YYYYMMDD + hyphen)
$DatePrefixRegex = '^[0-9]{8}-'

# Loop over each file in the local directory
Get-ChildItem -Path $LocalDir -File | ForEach-Object {
    $FilePath = $_.FullName
    $BaseName = $_.Name

    # Check if filename already starts with a date
    if ($BaseName -match $DatePrefixRegex) {
        $NewFilename = $BaseName
    } else {
        $NewFilename = "$Today-$BaseName"
    }

    Write-Host "Uploading $FilePath as s3://$BucketName/pending_processing/$NewFilename"

    $Attempt = 1
    $Success = $false

    while ($Attempt -le $MaxRetries) {
        $Output = aws s3 cp $FilePath "s3://$BucketName/pending_processing/$NewFilename" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Uploaded successfully on attempt $Attempt: $NewFilename"
            $Success = $true
            break
        } else {
            Write-Host "⚠️ Upload attempt $Attempt failed for $NewFilename."
            Write-Host "    Reason: $Output"
            $Attempt++
            Start-Sleep -Seconds 2
        }
    }

    if (-not $Success) {
        Write-Host "❌ Failed to upload after $MaxRetries attempts: $FilePath"
        $UploadFailed = $true
    }
}

# Exit code based on whether any upload failed
if ($UploadFailed) {
    Write-Host "Some files failed to upload."
    exit 1
} else {
    Write-Host "All files uploaded successfully."
    exit 0
}