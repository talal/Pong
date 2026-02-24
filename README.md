# Pong - PaF Gruppe I (WS 25/26)

This game was a group project for the "Patterns and Frameworks (PaF)" course at TH Lübeck.

## Authors

- Anwar, Muhammad Talal
- Lefhal-Lalaoui, Mohammed
- Omarov, Roman

## Screencast
<https://youtu.be/w9YyqTY9-Dc?si=huX6onfz6b0H9L9b>

## Backend (`/backend`)

Built with **Java 25** and **Spring Boot 3.5**.

Run using:

```sh
./mvnw clean spring-boot:run
```

- `src/main/java/paf_grp_i/pong/`
  - `controller/`: Handles incoming HTTP requests and WebSocket messages.
    - `GameController`: Manages WebSocket endpoints (`/app/game.join`, `/app/game.move`) for matchmaking and gameplay.
    - `JwtAuthController`: Handles REST endpoints for Signup (`/process_signup`) and Login (`/login`).
    - `UserController`: Handles authenticated user actions (Avatar upload, Password change).
    - `LeaderboardController`: Retrieves high score data.
    - `PongApiController`: General REST endpoints (e.g., user info).
  - `game/`: Contains the **Domain Model** for active game sessions (In-Memory).
    - `Game`: Represents the state of a running match (ball position, scores).
    - `GamePlayer`: Represents a player within a game session.
    - `GameState`: Enum for game status (WAITING, PLAYING, FINISHED).
  - `model/`: Contains **JPA Entities** for database persistence.
    - `User`: Maps to the `user` database table.
  - `repository/`: Data Access Layer.
    - `UserRepository`: Interface for CRUD operations on User entities.
  - `security/`: Configures Spring Security and JWT.
    - `WebSecurityConfig`: Defines the security filter chain (stateless, no form login).
    - `JwtTokenService` & `JwtAuthFilter`: Handles token generation and validation.
  - `service/`: The "Business Logic" layer.
    - `GameService`: The core game engine. Handles the game loop (~60 ticks/sec), physics collisions, and matchmaking queue.
  - `websocket/`: Real-time communication config.
    - `WebSocketConfig`: Configures STOMP over WebSocket.
    - `JwtStompChannelInterceptor`: Validates JWT tokens during WebSocket handshakes.

## Vue Frontend (`/frontend-vue`)

Built with **Vue 3**.

Run using:

```sh
npm run dev
```

- `src/`
  - `views/`: Vue Components representing distinct pages.
    - `Game.vue`: The main game client. Renders the `<canvas>` loop and captures keyboard input.
    - `Menu.vue`: Main page after login (also acts as homepage when not logged in).
    - `Login.vue` / `SignUp.vue`: Authentication forms.
    - `Leaderboard.vue`: Displays player rankings.
    - `ChangePassword.vue` / `ChangeAvatar.vue`: Forms for user profile management.
  - `auth.js`: A reactive state object for managing the JWT token and Fetch wrapper.
  - `router.js`: Client-side routing configuration (guards against unauthenticated access).
  - `main.js`: Application entry point.

## React Frontend (`/frontend-react`)

Built with **React** (Vite + React Router).

### Run
```sh
npm install
npm run dev
```

### Structure

- `src/`
  - `pages/`: React components representing distinct pages.
    - `Game.jsx`: Main game client. Renders the `<canvas>` loop, shows overlays (waiting / game over), connects to the STOMP websocket, and sends paddle movement.
    - `Menu.jsx`: Main page after login (also acts as homepage when not logged in).
    - `Login.jsx` / `SignUp.jsx`: Authentication forms.
    - `Leaderboard.jsx`: Displays player rankings.
    - `ChangePassword.jsx` / `ChangeAvatar.jsx`: Forms for user profile management.
  - `Theme.css`: Shared cyber-dark theme variables and base styles used across all pages.
  - `Menu.css` / `Login.css` / `SignUp.css` / `Leaderboard.css` / `ChangePassword.css` / `ChangeAvatar.css`: Page-specific styling files.
  - `auth.js`: Token store and helpers for saving, reading, and clearing the JWT.
  - `api.js`: Fetch wrapper for API calls (automatically adds the `Authorization` header).
  - `utils/`
    - `jwt.js`: Helpers for decoding the JWT payload (e.g. extracting username/email).
  - `main.jsx`: Application bootstrap and routing configuration.


## The Game Loop

The core of the application is a **Server-Authoritative Game Loop** running in `GameService.java`.

Instead of trusting the client (which could be hacked), the server calculates the ball position, physics, and scoring. The frontend merely displays the state it receives.

### How it works

1. **Tick:** The `@Scheduled(fixedRate = 17)` annotation triggers the loop approximately **60 times per second** (1000ms / 60 ≈ 16.6ms).
2. **Update:** For every active game, the server:
   - Updates the Ball position (`x = x + dx`, `y = y + dy`).
   - Checks for **Wall Collisions** (Top/Bottom).
   - Checks for **Paddle Collisions** (Left/Right).
   - Updates the **Score** if the ball passes a paddle.
   - Checks for **Win Condition** (First to 11 points).
3. **Broadcast:** The new state is sent via WebSocket to `/topic/game/{gameId}`.
4. **Render:** Both clients receive the exact same data and render the frame.
