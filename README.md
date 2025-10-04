# e-Course Backend System

# Prerequisites
- **Java JDK 17**
- **Leiningen 2.12**
- **MySQL 8.4.6**
- **Redis 7.0.15**
- **curl** or **Postman**

# Project Structure
```
be-ecourse-bit/
├── dev/
│   └── user.clj
├── src/
│   └── be_ecourse_bit/
│       ├── core.clj
│       ├── system/
│       │   ├── config.clj
│       │   └── components.clj
│       ├── domain/
│       │   ├── repositories/
│       │   │   ├── course.clj
│       │   │   └── user.clj
│       │   ├── services/
│       │   │   ├── auth.clj
│       │   │   └── course.clj
│       │   └── validators.clj
│       ├── http/
│       │   ├── handlers/
│       │   │   ├── home.clj
│       │   │   ├── auth.clj
│       │   │   ├── course.clj
│       │   │   └── debug.clj
│       │   ├── routes/
│       │   │   ├── home.clj
│       │   │   ├── auth.clj
│       │   │   ├── course.clj
│       │   │   └── debug.clj
│       │   ├── middleware.clj
│       │   └── router.clj
│       └── infrastructure/
│           ├── database.clj
│           ├── redis.clj
│           └── memory.clj
├── .vscode/
│   └── settings.json
├── project.clj
└── README.md
```

# Running the Application
## Production Mode
```bash
# Build uberjar
lein uberjar

# Run the application
java -jar target/uberjar/be-ecourse-bit-0.1.0-SNAPSHOT-standalone.jar
```
## Development Mode
### Option 1: Using Leiningen
```bash
lein run
```
### Option 2: Using REPL
```bash
lein repl

# In REPL
user=> (start)              ; Start server
user=> (dev-mode)           ; Start with auto-reload
user=> (status)             ; Check status
user=> (stop)               ; Stop server
```
### Option 3: Using VS Code + Calva
1. Open project in VS Code
2. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
3. Select "Calva: Start a Project REPL and Connect (aka Jack-In)"
4. Choose "e-Course Server (autoload)"
5. Wait for server to start