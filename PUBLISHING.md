# Publishing Process to Maven Central Portal

This guide provides step-by-step instructions for building, signing, packaging, and publishing a new release of **Hammock Sync** to **Maven Central Portal** (Sonatype) on both **Windows** and **Linux / macOS**.

---

## 1. Prerequisites

### 1.1 Java JDK 17
Ensure Java 17 is installed and `JAVA_HOME` is set:

- **Windows (PowerShell):**
  ```powershell
  $env:JAVA_HOME = "C:\Users\<username>\.jdks\temurin-17.0.20.1"
  ```
- **Linux / macOS (Bash):**
  ```bash
  export JAVA_HOME="/usr/lib/jvm/temurin-17"  # Adjust to your JDK 17 path
  ```

### 1.2 GnuPG (GPG)
- You must have an active, non-expired PGP key pair.
- The public key must be published to a keyserver recognized by Sonatype (e.g. `keyserver.ubuntu.com`):
  ```bash
  gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
  ```
- To extend key validity if expired:
  ```bash
  gpg --edit-key <KEY_ID>
  # Type 'expire', set new period (e.g. 2y), confirm, then 'save'
  gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
  ```

### 1.3 Gradle Signing Configuration (`~/.gradle/gradle.properties`)
In your user home directory (`~/.gradle/gradle.properties`), configure the signing properties:

- **Windows:** `C:\Users\<username>\.gradle\gradle.properties`
  ```properties
  signing.keyId=<LAST_8_CHARS_OF_KEY_ID>
  signing.password=<KEY_PASSPHRASE>
  signing.secretKeyRingFile=C:/Users/<username>/.gradle/secring.gpg
  ```
  Export the secret keyring in raw binary format:
  ```powershell
  gpg --batch --yes --output "C:\Users\<username>\.gradle\secring.gpg" --export-secret-keys <KEY_ID>
  ```
  *(Do not redirect with `>` in PowerShell, as it creates an invalid UTF-16 encoded file).*

- **Linux / macOS:** `~/.gradle/gradle.properties`
  ```properties
  signing.keyId=<LAST_8_CHARS_OF_KEY_ID>
  signing.password=<KEY_PASSPHRASE>
  signing.secretKeyRingFile=/home/<username>/.gradle/secring.gpg
  ```
  Export the secret keyring:
  ```bash
  gpg --batch --yes --output "$HOME/.gradle/secring.gpg" --export-secret-keys <KEY_ID>
  ```

---

## 2. Prepare the Version

1. Update the **`VERSION`** file at repository root with the release version (e.g. `1.1.0`). Make sure it **does not** contain `SNAPSHOT`.
2. *(Optional)* Update reference versions in `README.md` if necessary.

---

## 3. Build and Sign Artifacts Locally

Run Gradle `publishToMavenLocal`:

- **Windows:**
  ```powershell
  .\gradlew.bat clean publishToMavenLocal
  ```
- **Linux / macOS:**
  ```bash
  ./gradlew clean publishToMavenLocal
  ```

This builds and signs all 4 modules:
- `datastore-core`
- `datastore-android`
- `datastore-android-encryption`
- `datastore-javase`

Artifacts (`.jar`, `.aar`, `.pom`, `.module`, `-sources.jar`, `-javadoc.jar`, and signatures `.asc`) are output to `~/.m2/repository/org/hammock-sync/`.

---

## 4. Generate the Maven Central Portal Bundle ZIP

Maven Central Portal requires a ZIP archive that adheres to the following rules:
- Mirror the Maven repository folder structure (`org/hammock-sync/<module>/<version>/...`).
- Internal ZIP entry paths must use standard UNIX forward slashes (`/`).
- Include the module artifacts and their checksums (`.md5` and `.sha1`).
- **Do not** include directory-level metadata files like `maven-metadata-local.xml`.
- **Do not** generate checksums for `.asc` signature files.

### Option A: Windows (PowerShell Script)

```powershell
$version = (Get-Content "VERSION").Trim()
$m2Repo = "$HOME\.m2\repository\org\hammock-sync"
$tempDir = Join-Path $env:TEMP "central-bundle-$version"
$destZip = "$HOME\Desktop\hammock-sync-$version-bundle.zip"

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
if (Test-Path $destZip) { Remove-Item $destZip -Force }

$modules = @("datastore-android", "datastore-android-encryption", "datastore-core", "datastore-javase")

# 1. Copy only the release version files
foreach ($mod in $modules) {
    $targetDir = "$tempDir/org/hammock-sync/$mod/$version"
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Get-ChildItem -Path "$m2Repo\$mod\$version" -File | ForEach-Object {
        Copy-Item $_.FullName -Destination $targetDir -Force
    }
}

# 2. Generate md5 and sha1 checksums (excluding .asc, .md5, .sha1)
Get-ChildItem -Path $tempDir -Recurse -File | 
  Where-Object { $_.Extension -notmatch '\.(md5|sha1|asc)$' } | 
  ForEach-Object {
    $md5 = (Get-FileHash -Path $_.FullName -Algorithm MD5).Hash.ToLower()
    $sha1 = (Get-FileHash -Path $_.FullName -Algorithm SHA1).Hash.ToLower()
    [System.IO.File]::WriteAllText("$($_.FullName).md5", $md5)
    [System.IO.File]::WriteAllText("$($_.FullName).sha1", $sha1)
  }

# 3. Create the ZIP archive with UNIX forward slash paths
Add-Type -AssemblyName System.IO.Compression.FileSystem
$compressionLevel = [System.IO.Compression.CompressionLevel]::Optimal
$zipArchive = [System.IO.Compression.ZipFile]::Open($destZip, [System.IO.Compression.ZipArchiveMode]::Create)

$files = Get-ChildItem -Path $tempDir -Recurse -File
foreach ($file in $files) {
    $relativePath = $file.FullName.Substring($tempDir.Length + 1).Replace("\", "/")
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $file.FullName, $relativePath, $compressionLevel) | Out-Null
}
$zipArchive.Dispose()
Remove-Item $tempDir -Recurse -Force

Write-Output "Bundle ZIP created at: $destZip"
```

### Option B: Linux / macOS (Bash Script)

```bash
#!/usr/bin/env bash
set -euo pipefail

VERSION=$(tr -d ' \r\n' < VERSION)
M2_REPO="$HOME/.m2/repository/org/hammock-sync"
TEMP_DIR=$(mktemp -d /tmp/central-bundle-XXXXXX)
DEST_ZIP="$HOME/hammock-sync-${VERSION}-bundle.zip"

rm -f "$DEST_ZIP"

MODULES=("datastore-android" "datastore-android-encryption" "datastore-core" "datastore-javase")

# 1. Copy only files for this version
for MOD in "${MODULES[@]}"; do
    TARGET_DIR="$TEMP_DIR/org/hammock-sync/$MOD/$VERSION"
    mkdir -p "$TARGET_DIR"
    cp -f "$M2_REPO/$MOD/$VERSION"/* "$TARGET_DIR/"
done

# 2. Generate md5 and sha1 checksums (excluding .asc, .md5, .sha1)
find "$TEMP_DIR" -type f ! -name "*.asc" ! -name "*.md5" ! -name "*.sha1" | while read -r FILE; do
    if command -v md5sum >/dev/null 2>&1; then
        md5sum "$FILE" | awk '{print $1}' > "$FILE.md5"
    else
        md5 -q "$FILE" > "$FILE.md5"
    fi

    if command -v sha1sum >/dev/null 2>&1; then
        sha1sum "$FILE" | awk '{print $1}' > "$FILE.sha1"
    else
        shasum -a 1 "$FILE" | awk '{print $1}' > "$FILE.sha1"
    fi
done

# 3. Create the ZIP archive
(cd "$TEMP_DIR" && zip -r "$DEST_ZIP" org)
rm -rf "$TEMP_DIR"

echo "Bundle ZIP created at: $DEST_ZIP"
```

---

## 5. Upload and Publish on Maven Central Portal

1. Sign in to **[central.sonatype.com](https://central.sonatype.com)**.
2. Go to **Publish** or **Deployments**.
3. Click **Publish Component**.
4. Enter a deployment name (e.g., `hammock-sync-1.1.0`).
5. Select and upload the generated `hammock-sync-<version>-bundle.zip`.
6. Wait for automated validations to finish:
   - Checksum validation.
   - PGP signature verification against public keyservers.
   - POM & metadata completeness.
7. When the status displays **Validated**, click **Publish**.
8. The release will sync to Maven Central within 15–30 minutes.

---
