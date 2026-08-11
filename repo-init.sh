#!/data/data/com.termux/files/usr/bin/bash

# ============================================================
# Termux Android Project Initializer
# ============================================================
# Converts any new Android project to be Termux-compatible
# Run this after creating a new project or cloning one.
# ============================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() { echo -e "\n${BLUE}▶ $1${NC}"; }
print_ok() { echo -e "${GREEN}✓ $1${NC}"; }
print_warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }

# ============================================================
# Usage
# ============================================================
if [ $# -eq 0 ]; then
    echo "Usage: ./repo-init.sh <project-path-or-repo-url>"
    echo ""
    echo "Examples:"
    echo "  ./repo-init.sh ~/projects/MyNewApp"
    echo "  ./repo-init.sh https://github.com/user/MyApp.git"
    exit 1
fi

PROJECT_INPUT="$1"
PROJECT_DIR=""

# ============================================================
# Clone or use existing directory
# ============================================================
if [[ "$PROJECT_INPUT" == *.git ]]; then
    print_step "Cloning repository..."
    git clone "$PROJECT_INPUT"
    PROJECT_DIR=$(basename "$PROJECT_INPUT" .git)
    cd "$PROJECT_DIR"
else
    PROJECT_DIR="$PROJECT_INPUT"
    if [ ! -d "$PROJECT_DIR" ]; then
        print_error "Directory not found: $PROJECT_DIR"
        exit 1
    fi
    cd "$PROJECT_DIR"
fi

print_ok "Working in: $(pwd)"

# ============================================================
# Check if it's an Android project
# ============================================================
if [ ! -f "build.gradle" ] && [ ! -f "build.gradle.kts" ] && [ ! -f "settings.gradle" ] && [ ! -f "settings.gradle.kts" ]; then
    print_warn "This doesn't look like an Android project (no build.gradle or settings.gradle)"
    read -p "Continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# ============================================================
# 1. Fix Gradle Wrapper (USE GRADLE 8.6)
# ============================================================
print_step "Fixing Gradle wrapper to 8.6..."

if [ -f "gradle/wrapper/gradle-wrapper.properties" ]; then
    sed -i 's|distributionUrl=.*|distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip|g' \
        gradle/wrapper/gradle-wrapper.properties
    print_ok "gradle-wrapper.properties updated to Gradle 8.6"
else
    print_warn "gradle-wrapper.properties not found. Run 'gradle wrapper' first."
    mkdir -p gradle/wrapper
    cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
EOF
    print_ok "Created gradle-wrapper.properties with Gradle 8.6"
fi

# ============================================================
# 2. Add known-good libs.versions.toml (if missing)
# ============================================================
print_step "Setting up version catalog..."

mkdir -p gradle

if [ -f "gradle/libs.versions.toml" ]; then
    print_warn "libs.versions.toml already exists. Backing up to libs.versions.toml.bak"
    mv gradle/libs.versions.toml gradle/libs.versions.toml.bak
fi

cat > gradle/libs.versions.toml << 'EOF'
[versions]
agp = "8.4.0"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
coreKtx = "1.12.0"
lifecycle = "2.7.0"
activityCompose = "1.8.2"
composeBom = "2024.10.00"
navigationCompose = "2.7.7"
coroutines = "1.8.1"
room = "2.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
EOF

print_ok "Created libs.versions.toml with Termux-compatible versions"

# ============================================================
# 3. Create local.properties (SDK path)
# ============================================================
print_step "Creating local.properties..."

cat > local.properties << EOF
sdk.dir=$HOME/android-sdk
EOF

print_ok "local.properties created"

# ============================================================
# 4. Add gradle.properties (fix common errors)
# ============================================================
print_step "Adding gradle.properties..."

cat > gradle.properties << 'EOF'
org.gradle.daemon=false
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
EOF

# Add AIDL override if exists
if command -v aidl &>/dev/null; then
    echo "android.aidlExecutable=/data/data/com.termux/files/usr/bin/aidl" >> gradle.properties
    print_ok "Added AIDL override for Termux"
fi

print_ok "gradle.properties created"

# ============================================================
# 5. Fix compileSdk and targetSdk in build.gradle
# ============================================================
print_step "Fixing compileSdk/targetSdk to 35..."

# For build.gradle.kts
if [ -f "app/build.gradle.kts" ]; then
    sed -i 's/compileSdk = [0-9]*/compileSdk = 35/g' app/build.gradle.kts
    sed -i 's/targetSdk = [0-9]*/targetSdk = 35/g' app/build.gradle.kts
    print_ok "Updated app/build.gradle.kts"
fi

# For build.gradle (Groovy)
if [ -f "app/build.gradle" ]; then
    sed -i 's/compileSdk [0-9]*/compileSdk 35/g' app/build.gradle
    sed -i 's/targetSdk [0-9]*/targetSdk 35/g' app/build.gradle
    print_ok "Updated app/build.gradle"
fi

# ============================================================
# 6. Add INTERNET permission (if missing)
# ============================================================
print_step "Adding INTERNET permission..."

if [ -f "app/src/main/AndroidManifest.xml" ]; then
    if ! grep -q "INTERNET" app/src/main/AndroidManifest.xml; then
        sed -i '/<manifest/a\    <uses-permission android:name="android.permission.INTERNET" />' \
            app/src/main/AndroidManifest.xml
        print_ok "Added INTERNET permission"
    else
        print_ok "INTERNET permission already exists"
    fi
fi

# ============================================================
# 7. Generate wrapper files
# ============================================================
print_step "Generating Gradle wrapper..."

if command -v gradle &>/dev/null; then
    gradle wrapper --gradle-version 8.6 2>/dev/null || true
    print_ok "Gradle wrapper generated"
else
    print_warn "Gradle not found. Run 'gradle wrapper' manually after this script."
fi

# ============================================================
# 8. Try to build (optional)
# ============================================================
print_step "Attempting test build..."

if [ -f "gradlew" ]; then
    chmod +x gradlew
    if ./gradlew --version &>/dev/null; then
        print_ok "Gradle wrapper works!"
    else
        print_warn "Gradle wrapper test failed. Try running manually."
    fi
fi

# ============================================================
# 9. Add reasonix-friendly .vscode settings
# ============================================================
print_step "Creating VS Code settings..."

mkdir -p .vscode
cat > .vscode/settings.json << 'EOF'
{
    "java.compile.nullAnalysis.mode": "automatic",
    "java.configuration.updateBuildConfiguration": "interactive",
    "kotlin.codeCompletion.enabled": true,
    "files.associations": {
        "*.kts": "kotlin"
    }
}
EOF

print_ok "VS Code settings created"

# ============================================================
# 10. Done
# ============================================================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}✅ REPO INITIALIZED FOR TERMUX!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "📁 Project: $(pwd)"
echo ""
echo "📌 What was done:"
echo "   ✅ Gradle 8.6 (wrapper)"
echo "   ✅ libs.versions.toml (AGP 8.4.0, Kotlin 2.0.20)"
echo "   ✅ local.properties (SDK path)"
echo "   ✅ gradle.properties (AAPT2, AIDL overrides)"
echo "   ✅ compileSdk/targetSdk → 35"
echo "   ✅ INTERNET permission"
echo "   ✅ VS Code settings"
echo ""
echo "🚀 Next steps:"
echo "   ./gradlew assembleDebug --no-daemon"
echo ""
echo "📦 To copy APK:"
echo "   cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/"
echo ""
echo -e "${GREEN}Happy vibecoding! 🚀${NC}"
