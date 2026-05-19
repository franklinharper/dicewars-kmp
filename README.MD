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
./scripts/run-tournament --bots rebel,turtle,bully,max --rounds 100 --seed 1
```

Available bot IDs and strategies:

| Bot ID | Display name | Strategy |
|---|---|---|
| `rebel` | Rebel | Targets the leading player. If one player has more than 40% of all dice, Rebel only attacks battles involving that leader. It can take equal-dice attacks when the current player or target is top-ranked, or when a random aggression check passes. |
| `turtle` | Turtle | Conservative bot. Avoids attacks that are not clearly favorable, avoids attacking when a stronger friendly neighbor could attack instead, and avoids weakening vulnerable positions when established and out of reserve. |
| `bully` | Bully | Randomly chooses among legal adjacent attacks where the attacker has strictly more dice than the defender. |
| `emperor` | Emperor | Strategic search bot. Uses cautious-style filters, exact battle win probabilities, and a 3-ply expectiminimax search to choose attacks that improve a board-strength evaluation. |
| `frontier-commander` | Frontier Commander | Tournament-focused expected-value bot. Evaluates success and failure board states for every attack, applies tactical adjustments for leaders, eliminations, pressure, and connectivity, then attacks only above a dynamic threshold. |
| `max` | Max | Uses the same policy as human autoplay. It attacks only when attacker dice are at least defender dice, reserves plus reinforcements can refill all owned territories to 8 after either outcome, and chooses the most favorable qualifying attack. |

Useful options:

```bash
# CSV output to stdout
./scripts/run-tournament --bots rebel,turtle,max --rounds 100 --format csv

# Write a report to a file
./scripts/run-tournament --bots rebel,turtle,max --rounds 100 --out build/tournament.txt

# Run rounds concurrently. By default this uses CPU count minus 1.
./scripts/run-tournament --bots rebel,turtle,max --rounds 1000

# Override parallelism explicitly
./scripts/run-tournament --bots rebel,turtle,max --rounds 1000 --parallel 4

# Capture action logs for failed rounds
./scripts/run-tournament --bots rebel,turtle,max --rounds 100 --log-failed-rounds
```

The wrapper rebuilds the tournament CLI only when its source inputs changed, then runs the generated fat JAR from `dist/tournament-cli/`.

Replay a reported round:

```bash
./scripts/replay-round --round-seed 123 --seats rebel,turtle,max --max-actions 100000 --last-steps 50
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

