<p align="center">
  <img src="shared/src/commonMain/composeResources/drawable/Gymetrics_Logo.png" width="96" alt="Gymetrics logo" />
</p>

<h1 align="center">Gymetrics</h1>

<p align="center">
  A self-hosted workout tracker for Android and iOS.<br/>
  You run the backend and AI Service on your own server, the app talks to it, and your training data stays with you.
</p>

<p align="center">
  <a href="https://github.com/Jakob-ui/Gymetrics_App/releases">Download APK</a> ·
  <a href="https://github.com/Jakob-ui/Gymetrics-Backend">Backend repo</a> ·
  <a href="https://gymetrics.at">gymetrics.at</a>
</p>

This repo is the mobile app. The backend (NestJS, MongoDB and the Python scraper) lives in Gymetrics-Backend.

Running it

1. Backend. You need a running Gymetrics Backend first. Setup with Docker Compose is described in the [backend](https://github.com/Jakob-ui/Gymetrics-Backend) repo. There's also a compose generator, where you can choose which services you want on [gymetrics.at](https://gymetrics.at).

2. App.​
* Android: download the latest APK from Releases. Needs Android 14 or newer.
* iOS build it yourself:​ open iosApp/ in Xcode and run  ```./gradlew :androidApp:assembleDebug``` from there.​
* On first launch, enter your server URL, then register.

## Why I built this.

Mostly I've built it as an University Project to learn more about Hybrid Mobile Developement with Kotlin Multiplatform. Also as someone with an IT-background from my bachelorsdegree, I know how many applications are not very secure and respect your data. For this reason I startet out to create a rather simple looking Frontend App that can connect to a backend and database runnning on ones own server. In late August 2026 I started the app, which was also the first time I wrote Kotlin. I had never used Kotlin Multiplatform before either, so a big part of this project was figuring out how KMP actually works: source sets, expect/actual, coroutines, Compose state. It was very important for me to also have a caching option to also read Data when my device is offline.


## What it does

Connect to your own server. On first launch you enter your server URL (via a registered Domain or just use a VPN). The app pings it and checks that the response really comes from a Gymetrics backend, not just that it returns a 200 (pretty much everything returns 200). For not that tech savy persons I also provide a ready made solution under "app.gymetrics.at", which defeats the purpose a little bit, but is handy to just try the app.
Then you can log in (or register an account). Access and refresh tokens are stored in the platform's secure storage, and expired tokens are refreshed automatically.
Templates. Then you can Build workout templates (even with AI if you've configured it that way) with exercises, target reps and weights.
For helping with generating templates with AI you can add personal information about yourself in the personal settings and pick your gym. The backend then runs an LLM loop and calls a local LLM (via Ollama) to build a plan based on your recent trainings, your biometric Data and the machines your gym actually has.
You then can schedule trainings from your templates on specific days and see them in a month view.
As mentioned before everything works offline. Everything you've loaded is cached on the device. Creating, deleting and completing trainings also works offline and syncs once you're back online.


## Architecture

```mermaid
flowchart LR
    App["Gymetrics App<br/>(Android / iOS)"]

    subgraph Server["Your server (Docker Compose)"]
        API["Backend<br/>NestJS · REST · JWT"]
        DB[("MongoDB")]
        Scraper["Scraper<br/>Python · Flask"]
        MCP["MCP Server<br/>Streamable HTTP"]
    end

    Ollama["Ollama<br/>local LLM"]
    Gym["Gym website<br/>(FitInn)"]

    App -- "HTTPS / JSON" --> API
    API --> DB
    API -- "monthly cron" --> Scraper
    Scraper -- "studios & equipment" --> Gym
    API -- "chat + tool calls" --> Ollama
    MCP -- "REST" --> API
```

The app only ever talks to the backend. The backend stores everything in MongoDB, and once a month it asks the scraper service to pull the list of gyms and their equipment from the FitInn website. When you generate a template with AI, the backend sends your request to Ollama together with two tools the model can call itself: one that loads your recent trainings and one that loads the equipment of your gym. The MCP server exposes the same kind of data over the Model Context Protocol, so other MCP clients can use it too. (I know, defeats the purpose of the self contained data, but it was fun to learn how MCPs work)

A more detailed diagram with the data model is in the [backend repo](https://github.com/Jakob-ui/Gymetrics-Backend)


## Inside the app

```mermaid
flowchart TD
    Screens["Compose screens"] --> VMs["ViewModels<br/>(sealed UI state per screen)"]
    VMs --> Repos["Repositories"]

    Repos --> Http["Ktor HttpClient<br/>bearer auth + auto refresh"]
    Repos --> Cache[("KStore<br/>JSON files on device")]
    Repos --> Queue["Pending action queue"]

    Conn["ConnectivityObserver<br/>(expect / actual)"] --> Sync["SyncManager"]
    Sync -- "replays when online" --> Queue
    Tokens["TokenStore<br/>KVault (Keychain / encrypted prefs)"] --> Http
    Session["SessionManager<br/>startup routing"] --> Http
```
The app follows MVVM. Most features have their own folder under ui/ with a screen, a ViewModel and a sealed State class. ViewModels never touch the network directly. They go through repositories, which decide whether to serve cached data, fetch fresh data, or queue a write for later. Everything is wired together with Koin (to take advantage of dependency injection).

```
shared/src/
├── commonMain/    ← about 97% of the code: UI, ViewModels, repositories, networking, sync
├── androidMain/   ← platform bits: secure storage, connectivity, dynamic color, status bar
└── iosMain/       ← the same for iOS
androidApp/        ← Android entry point
iosApp/            ← iOS entry point (Xcode project)
```


## Decisions I made and why

Kotlin Multiplatform with a shared Compose UI: one codebase for Android and iOS, including the UI. I chose this framework because I find it very interesting. It compiles natively on iOS and runs on the JVM on Android. It was also a great opportunity to learn Kotlin.

In the beginning, I started by reading documentation and creating the welcome screen, the state machine behind the JWT token storage logic, and the HTTP interceptor with Ktor. I decided not to use dependency injection at the start because I wanted to understand the fundamentals first, although I planned to introduce it later. I wanted to understand the problem before using a tool designed to solve it.

Of course, I also used AI to help me code, but I decided to conduct a small experiment with an M4 Mac I had lying around. I installed [OMLX](https://omlx.ai/) to host local models on the Mac and set up a VPN so I could call the API even when I was away from home. I then combined a quantized Qwen3-Coder model with a [Context7](https://context7.com/) MCP connector, using the current KMP documentation to provide up-to-date coding solutions. This worked very well during the early stages of development. As a frontend wrapper, I used the sadly no-longer-maintained VS Code extension Continue because it provided every tool I needed. Unfortunately, VS Code does not offer robust support for locally hosted coding models, and I did not want to use an agentic tool such as OpenCode. After developing with this setup for a while, the context became too large for my 16 GB MacBook, so I had to switch to Claude.


### For storing different things I chose: 

* Tokens in secure storage, not in settings. Access and refresh tokens go into KVault, which uses the Keychain on iOS and encrypted storage on Android. 
* Normal preferences (theme, server URL) go into plain settings. 

* Offline cache with KStore instead of a database. My original plan was SQLDelight. I ended up storing the API responses as JSON files with KStore, one file per resource to keep it simple for caching purposes and time reasons.

* A queue for offline writes. If you create, delete or complete a training while offline, the action is stored in a persistent queue (a sealed PendingAction type saved to storage). The SyncManager listens for connectivity changes and replays the queue in order once you're back online. If one action fails, it stops there instead of skipping ahead, because the order matters (you can't complete a training the server doesn't know about yet). 


### AI implementation

For AI generative content I focused on a distinct strategy. AI generation runs in the background (on my deployed version it's a seperate Ollama docker). Small local models are slow (especially on old hardware), and the app uses a 10 second request timeout. So the backend answers right away with a template marked as generating and does the actual work in the background. The app checks every 3 seconds until it's done, and you can leave the screen in the meantime.


## Tech stack

#### Language
* Kotlin 2.4
#### UI
* Compose Multiplatform 1.11
* Material 3
#### Local storage
* KStore: Cache and offline request queue
* KVault: Secure token storage
* Multiplatform Settings: Application settings and preferences
#### CI
* GitHub Actions: builds the APK and publishes a release for every v* tag. 
* Branch protection rules: Configured to protect important branches.


## What I'd do differently

If I would build this application again I would do some things differently:
* Every repository has its own version of "load from cache, fetch, write back". I'd pull that into one generic helper and distinguish between different actions it has to take
* I would'nt choose to do the same UI components for android and iOS beacause it just feels a little bit sluggish and off on the iOS version.
* Next time I would use a mirrored variant of the online database (didn't went as planned because I used a no-Sql database in the backend, next time maybe a relational database :) )
* I also would add tests in the CI/CD Pipeline to validate every function.
* Adding one place of ground truth in the future (jumped a little bit to much from note tool to note tool in this project, which ended up in very confusing docs)


## What's next/Features I want to add (let's see)

Different workout modes, e.g. going through exercises one by one with a running timer
Time tracking per workout
Support for more gyms
Invalidating JWTs on logout (blacklist on the backend)
Icons/Pictures for templates
An easier way to move a training to another day

## Branding

The Gymetrics name and logo are not covered by the software license and may not be used to promote derivative projects without permission.

## License

This project is licensed under the Apache License 2.0.. See the [LICENSE](LICENSE) file for details.
