# Pong - PaF Gruppe I (WS 25/26)

## Authors

- Anwar, Muhammad Talal (muhammadtalal.anwar@stud.th-luebeck.de)
- Lefhal-Lalaoui, Mohammed (mohammed.lefhal-lalaoui@stud.th-luebeck.de)
- Omarov, Roman (roman.omarov@stud.th-luebeck.de)

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
    - `PongApiController`: General REST endpoints (e.g., user info).
  - `game/`: Contains the **Domain Model** for active game sessions (In-Memory).
    - `Game`: Represents the state of a running match (ball position, scores).
    - `GamePlayer`: Represents a player within a game session.
    - `GameState`: Enum for game status (WAITING, PLAYING, FINISHED).
  - `model/`: Contains **JPA Entities** for database persistence.
    - `User`: Maps to the `app_user` database table.
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
    - `Menu.vue`: Main hub after login.
    - `Login.vue` / `SignUp.vue`: Authentication forms.
  - `auth.js`: A reactive state object for managing the JWT token and Fetch wrapper.
  - `router.js`: Client-side routing configuration (guards against unauthenticated access).
  - `main.js`: Application entry point.

## React Frontend (`/frontend-react`)

TODO...

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
   - Checks for **Win Condition** (First to 11).
3. **Broadcast:** The new state is sent via WebSocket to `/topic/game/{gameId}`.
4. **Render:** Both clients receive the exact same data and render the frame.
