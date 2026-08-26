# ⚽ Football Career Manager

**Football Career Manager** is a personal project developed in Java with the goal of creating a football management and career simulation game inspired by games such as Football Manager.

The project is being built **from scratch as a learning project between university courses**, using the development process to put into practice concepts from my Computer Engineering degree while learning new technologies and improving my software engineering skills.

The project is being developed with the assistance of **ChatGPT (GPT-5.6 Luna)**, which I use as a development and learning assistant for architecture, programming, database design, debugging, testing and technical explanations. The goal is not simply to generate code, but to understand what is being built and why.

---

## 🎯 Project Goal

The long-term goal is to create a playable football career where the player can:

- Manage a football club.
- Build and manage a squad.
- Develop players.
- Play and simulate matches.
- Manage competitions and league tables.
- Make transfers and manage contracts.
- Progress through multiple seasons.
- Experience a football world that continues to evolve over time.

The project will grow progressively, starting with a small and solid foundation and adding more complex systems as development continues.

---

## 🛠️ Technologies

- **Java 21**
- **JavaFX**
- **Maven**
- **JUnit 5**
- **SQLite**
- **JDBC**
- **Git / GitHub**
- **Visual Studio Code**

---

## 🏗️ Architecture

The project is being built with a separation between the main responsibilities of the application:

```text
JavaFX
   │
   ▼
Game Logic / Services
   │
   ▼
Repositories
   │
   ▼
SQLite

🗄️ Database

SQLite is used as the local relational database.

The database schema is located at:

src/main/resources/schema.sql

The current schema contains 11 main tables covering:

Players
Teams
Leagues
Seasons
Competitions
Matches
Player statistics
League standings
Transfers / club history
Careers

The project uses JDBC and a repository layer to keep database access separate from the domain model.

🧪 Testing

The project uses JUnit 5 to test the domain model and database functionality.

Current status:

17 / 17 tests passing
BUILD SUCCESS

Automated tests will continue to grow alongside the project.

📈 Current Status

The initial foundation of the project is complete.

The project currently has:

Java and Maven configured.
JavaFX configured.
A domain model.
Automated tests.
A relational SQLite database.
JDBC integration.
Database initialization.
A repository layer.
Git/GitHub version control.

The next stage is to connect these foundations and start building the actual career simulation and user interface.

🚀 Vision

This project is intentionally being developed step by step.

The final result does not need to reproduce every feature of a commercial football management game. The objective is to build a substantial personal project, understand the technologies behind it, and gradually turn the initial idea into a working football career simulation.

Build it. Understand it. Improve it. ⚽