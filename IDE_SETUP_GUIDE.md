# IntelliJ IDEA Setup Guide

If Maven is not being detected and you can't navigate to class definitions, follow these steps:

## Quick Fix (Try This First)

### 1. **Close IntelliJ Completely**
```bash
# Force quit IntelliJ
pkill -f "IntelliJ IDEA"

# Or use Activity Monitor on Mac to quit the app
```

### 2. **Clear IntelliJ Cache**
```bash
# On macOS:
rm -rf ~/Library/Caches/JetBrains/IntelliJIdea*
rm -rf ~/Library/Application\ Support/JetBrains/IntelliJIdea*

# Or use IntelliJ's built-in option:
# File → Invalidate Caches → Invalidate and Restart
```

### 3. **Reopen the Project**
- Open IntelliJ IDEA
- Click "Open" → Select the project root directory: `/Users/suhasdeshmukh/Projects/personal/Stocks Analyser`
- Wait for indexing to complete (watch bottom status bar)

---

## If That Doesn't Work

### Step 1: Verify Maven Installation
```bash
mvn --version
# Should output Maven 3.8.x and Java 25
```

### Step 2: Configure JDK in IntelliJ
1. **IntelliJ → Settings → Project Structure → Project**
2. Set "SDK" to **Java 25** (should show as "openjdk-25")
3. If not available, click "Add SDK" → "Add JDK" → Select `/Library/Java/JavaVirtualMachines/openjdk-25.jdk/Contents/Home`

### Step 3: Reload Maven Projects
1. **View → Tool Windows → Maven** (or press `⌘9` on Mac)
2. You should see a "Maven" panel on the right
3. In the Maven panel, look for the root project or "stock-signal-service"
4. **Right-click → "Reload Projects"**
5. Wait for Maven to re-index all dependencies

### Step 4: Manually Link pom.xml
If Maven panel is empty:
1. **File → Open** → Navigate to `backend/pom.xml`
2. IntelliJ will ask "Link Maven project?"
3. Click **"Link Project"**
4. Wait for Maven to download dependencies

### Step 5: Enable Annotation Processing
1. **IntelliJ → Settings → Build, Execution, Deployment → Compiler → Annotation Processors**
2. ✅ Check **"Enable annotation processing"**
3. ✅ Check **"Obtain processors from project classpath"**
4. Apply and OK

### Step 6: Rebuild Project
1. **Build → Rebuild Project**
2. Watch the "Build" tab for completion
3. You should see: "Build completed successfully"

---

## Troubleshooting Maven Detection

### Maven Not Showing in Tool Window
**Solution**:
- Right-click `backend/pom.xml` in Project view
- Select **"Configure" → "Convert to Maven Project"**
- Or drag `backend/pom.xml` into Maven tool window

### Classes Not Found / Red Squiggles
**Solution 1 - Invalidate Cache**:
1. **File → Invalidate Caches**
2. Select **"Invalidate and Restart"**

**Solution 2 - Reimport Maven**:
1. Open Maven tool window
2. Click the refresh icon (circular arrow)
3. Select root "stock-signal-service" project
4. Click "Reload All Maven Projects"

**Solution 3 - Manual Reimport**:
1. **File → Open** → `backend/pom.xml`
2. **File → Project Structure**
3. Go to **"Modules"** → **"+" (Add)** → **"Import Module"**
4. Select `backend/pom.xml`
5. Follow the wizard to import

### Cannot Navigate to Spring Boot Classes
**Solution**:
1. Go to **View → Tool Windows → Maven**
2. Expand the project tree
3. Right-click on "Lifecycle"
4. Click "clean" first, then "package"
5. Wait for download to complete

---

## Verify Setup is Working

### ✅ All of these should be true:

1. **Maven Tool Window Shows Projects**
   - View → Tool Windows → Maven
   - Should see "stock-signal-service" with dependencies

2. **pom.xml is Recognized**
   - Open `backend/pom.xml`
   - Top-right should show "Maven" label
   - No red squiggles in the XML

3. **Can Navigate to Classes**
   - Open `backend/src/main/java/com/suhas/stocktracker/StockSignalServiceApplication.java`
   - `Ctrl+Click` (or `Cmd+Click` on Mac) on any Spring class
   - Should navigate to the class definition

4. **Can See Spring Boot Run Configuration**
   - **Run → Edit Configurations**
   - Should see "Backend: Spring Boot" pre-configured
   - Or add one: **+ → Spring Boot**

5. **No Errors in Bottom Right**
   - Status bar should say "No files to analyze"
   - Or show completed indexing

---

## Running Backend from IntelliJ

### Via Run Configuration
1. **Run → Run 'Backend: Spring Boot'** (or press `^R` on Mac)
2. Or click the green play button next to the configuration name

### Via Maven Tool Window
1. Open **Maven** tool window
2. Expand project → **Lifecycle**
3. Double-click **spring-boot:run**

### Via Terminal in IDE
```bash
# In IntelliJ's built-in Terminal
cd backend
mvn spring-boot:run
```

---

## If All Else Fails

### Nuclear Option - Fresh Project Open
```bash
# From command line:
cd /Users/suhasdeshmukh/Projects/personal/Stocks\ Analyser

# Tell IntelliJ to reimport from scratch
rm -rf .idea/modules
rm -rf .idea/libraries
rm -rf .idea/artifacts
rm .idea/misc.xml
rm .idea/workspace.xml
```

Then:
1. Close IntelliJ
2. Reopen the project
3. IntelliJ will regenerate all `.idea` files
4. Follow Steps 1-5 from "If That Doesn't Work" above

---

## Getting Help

If you're still stuck:
1. Check **View → Tool Windows → Maven** → Right panel "Problems"
2. Look for error messages
3. Check **View → Tool Windows → Run** → Look for build errors
4. Run: `mvn clean install` in terminal to see detailed errors
5. Search error message on Stack Overflow

---

## Common Error Messages & Fixes

### "SDK 'openjdk-25' not found"
- **File → Project Structure → Project → SDK**
- Click on the SDK field
- Click "Add SDK" → "Add JDK"
- Browse to `/Library/Java/JavaVirtualMachines/openjdk-25.jdk`

### "Cannot resolve symbol" for Spring classes
- **Build → Rebuild Project**
- Wait for Maven to sync dependencies
- If still failing: Maven tool window → Reload All

### "Annotation processing is not enabled"
- **Settings → Build, Execution, Deployment → Compiler → Annotation Processors**
- ✅ Enable annotation processing
- Apply → Rebuild project

### pom.xml shows "Plugin execution not covered"
- This is a warning, not an error
- Can be ignored or suppressed in POM
- Won't affect compilation or execution

---

## Verify Maven Dependency Download

If dependencies aren't downloading:
```bash
cd backend
mvn clean install

# If it fails, try:
mvn -U clean install  # Force update snapshots
```

This will:
1. Download all dependencies
2. Compile the project
3. Run tests (if available)
4. Package the JAR

If it succeeds in terminal but fails in IntelliJ, then it's a cache issue:
- **File → Invalidate Caches → Invalidate and Restart**

---

## Testing Backend Connection in IDE

Once Maven is properly detected:

1. **Open `StockSignalServiceApplication.java`**
2. **Right-click on the class**
3. **"Run 'StockSignalServiceApplication'"**
4. You should see in the Run window:
   ```
   ___________        ______________
   |           \      /              |
   | SPRING     \____/    BOOT       |
   |                                |
   Application should start
   Listening on port 8080
   ```

5. Open http://localhost:8080/api/health in browser
6. Should see: `{"status":"UP"}`

---

## Last Resort

If nothing works, post the error from:
1. **View → Tool Windows → Run** (tab: "Backend: Spring Boot")
2. Or terminal output from `mvn clean install`
3. Include the exact error message

The most common issue is:
- **Java 25 not being set as project SDK**
- **Maven cache being corrupted** (fixed by invalidating caches)
- **pom.xml not being recognized** (fixed by re-importing module)
