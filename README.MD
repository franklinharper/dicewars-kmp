# dicewars-port

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration  

To build the application bundle:  
 - run `./gradlew :androidApp:assembleDebug`  
 - find `.apk` file in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`  

### Desktop
Run the desktop application: `./gradlew :desktopApp:run`  
Run the desktop **hot reload** application: `./gradlew :desktopApp:hotRun --auto`  

### Bot Tournaments
Run a headless bot tournament from the command line:

```bash
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger,max --rounds 100 --seed 1
```

Available bot IDs:

- `target-leader` — Rebel
- `cautious` — Turtle
- `attack-when-stronger` — Berzerker
- `strategic` — Emperor
- `frontier-commander` — Frontier Commander
- `max` — Max

Useful options:

```bash
# CSV output to stdout
./scripts/run-tournament --bots target-leader,cautious,max --rounds 100 --format csv

# Write a report to a file
./scripts/run-tournament --bots target-leader,cautious,max --rounds 100 --out build/tournament.txt

# Run rounds concurrently
./scripts/run-tournament --bots target-leader,cautious,max --rounds 1000 --parallel 4

# Capture action logs for failed rounds
./scripts/run-tournament --bots target-leader,cautious,max --rounds 100 --log-failed-rounds
```

The wrapper rebuilds the tournament CLI only when its source inputs changed, then runs the generated fat JAR from `dist/tournament-cli/`.

Replay a reported round:

```bash
./scripts/replay-round --round-seed 123 --seats target-leader,cautious,max --max-actions 100000 --last-steps 50
```

For all CLI options:

```bash
./scripts/run-tournament --help
```

### iOS
To run the application on iPhone device/simulator:  
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration  
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio  

### Web Distribution
Build web distribution: `./gradlew :webApp:composeCompatibilityBrowserDistribution`  
Deploy a dir `webApp/build/dist/composeWebCompatibility/productionExecutable` to a web server  

### Wasm Browser
Run the browser application: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`  

