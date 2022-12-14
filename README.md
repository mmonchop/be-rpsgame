# RPSGame - backend
### Coding Challenge: Miguel Moncho

## Functional Added Features (26-Sep)
- 3 game modes:
  - VS_MACHINE: Opponent moves are generated randomly by machine
  - VS_FRIEND: Posible to play with a friend in a created game room
  - VS_RANDOM_PLAYER: Posibility to play with another random player
- Possibility to play many games with same player in created game room
- Each game ends when one player wins 3 rounds (configurable)
- Custom Metrics in Prometheus:
  - `rpsgame-rooms-created-count`: Number of game rooms created
  - `rpsgame-invites-accepted-count`: Number of rooms play invitations accepted (VS_FRIEND mode)
  - `rpsgame-invites-accepted-time`: Time between player 1 created room and player 2 accepted invitation (VS_FRIEND mode)
  - `rpsgame-games-created-count`: Number of games created
  - `rpsgame-games-over-count`: Number of games finished
  - `rpsgame-games-over-time`: Game duration time
  - `rpsgame-rounds-played-count`: Number of rounds played
  - `rpsgame-rounds-over-count`: Number of rounds finished
  - `rpsgame-turns-played-count`: Number of turns played
- Multi-language

## Technical Added Features (26-Sep)
- OpenAPI documentation
- REST API secured using Basic Auth
- Websockets capability (update events between 2 players - VS_FRIEND or VS_RANDOM_PLAYER modes)
- Application Metrics to Prometheus
- Information persisted using MongoDB database
- Integration Tests & Unit Tests
- Dto mapping using MapStruct
- JaCoCo reports
- Included 2 PlantUML diagrams:
  - `plant-uml/rpsgame-model.puml` (Model Class diagram)
  - `plant-uml/rpsgame-services.puml` (Services Class diagram)

**_New features added (1-Oct)_**
- Gatling simulations:
  - Create room: `gatling/simulations/rps-game-create-room-dev.scala`
  - Stress test simulation results in `gatling/results` (.zip file)
- Notifications when player leaves game (`window:beforeunload` events)  

**_New features added (16-Oct)_**
- REST API supports OAuth 2.0 Authentication Code flow with PKCE. JWT tokens
- Secure websockets API

## Maven - Run

```sh
clean install jacoco:report spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```
### Environment Variables required (profile=dev):
REST API is protected with **Basic Auth**, following ENV variables need to be provided in order to create a default user with Spring Security.

| Name | Value | Comments |
| ------ | -- | -- |
| API_REST_USERNAME | rpsplayer | User is created in `InMemoryUserDetailsManager` |
| API_REST_PASSWORD | password | Password for user created in memory |

#### Configuration using OAuth 2.0 security (profile=dev-oauth2):
Authorization Code flow with PKCE using Azure AD (password / secrets to be provided in Slack chat)
```sh
clean install jacoco:report spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev-oauth2
```

| Name | Value | Comments |
| ------ | ------ | -- |
| API_REST_USERNAME | user@mmonchopgmail.onmicrosoft.com | |
| API_REST_PASSWORD | ****** | To be provided in Slack |
| AZURE_AD_CLIENT_SECRET | ****** | To be provided in Slack |

## Class diagram (Services):
- Main classes in services layer:
  - `RoomService`: Manage rooms (creation, invitations, ...)
  - `GamePlayService`: Manage a RPS game (game, rounds, turns, etc.)
  - `RoundResultService`: Calculate winner in a round
  - `MetricsService`: Manage custom metrics to Prometheus
  - `NotificationService`: Send game events over websockets (Stomp)

<img src="./plant-uml/rps-game-services.png" />

## Class diagram (Model):
<img src="./plant-uml/rps-game-model.png" />
