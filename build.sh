#!/bin/bash
# Builds BabyPlayer.apk with the plain Android SDK tools (no Gradle).
#
# Environment variables (all optional):
#   ANDROID_HOME    Android SDK location            (default: ~/Android/Sdk)
#   BUILD_TOOLS     build-tools version             (default: 35.0.0)
#   PLATFORM        platform to compile against     (default: android-35)
#   KEYSTORE        signing keystore, created if missing (default: ./release.keystore)
#   KEYSTORE_PASS   keystore password               (default: babyplayer)
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$SDK/build-tools/${BUILD_TOOLS:-35.0.0}
JAR=$SDK/platforms/${PLATFORM:-android-35}/android.jar
KEYSTORE=${KEYSTORE:-$PWD/release.keystore}
KEYSTORE_PASS=${KEYSTORE_PASS:-babyplayer}

rm -rf build && mkdir -p build/gen build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml -o build/unsigned.apk \
    --java build/gen --min-sdk-version 26 --target-sdk-version 31 \
    --version-code 2 --version-name 1.1 build/res.zip

javac -nowarn --release 17 -cp "$JAR" -d build/classes \
    $(find src build/gen -name '*.java')
"$BT/d8" --lib "$JAR" --min-api 26 --release --output build/dex \
    $(find build/classes -name '*.class')
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)

"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -alias babyplayer -keyalg RSA -keysize 2048 \
        -validity 36500 -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" -dname "CN=BabyPlayer"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASS" \
    --v4-signing-enabled false --out BabyPlayer.apk build/aligned.apk

echo "Built $PWD/BabyPlayer.apk"
