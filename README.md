# Padel Manager

![Backend CI](https://github.com/ayoubglb/PadelManager/actions/workflows/backend-ci.yml/badge.svg)
![Frontend CI](https://github.com/ayoubglb/PadelManager/actions/workflows/frontend-ci.yml/badge.svg)

## Pile technologique

| Couche     | Technologie                                                      |
|------------|------------------------------------------------------------------|
| Base       | SQL Server 2022 (Docker)                                         |
| Backend    | Java 25, Spring Boot 4.0.5, JPA/Hibernate, Liquibase             |
| Frontend   | Angular 21, Angular Material, Tailwind CSS 4                     |
| Build      | Maven (back), npm (front)                                        |
| Tests      | JUnit + Mockito + Testcontainers (back), Vitest + Cypress (front)|
| CI/CD      | GitHub Actions                                                   |

## Prérequis

- **Docker** ≥ 24 et **Docker Compose** ≥ 2.20
- **Java 25** 
- **Maven** ≥ 3.9 (inclus via le wrapper `mvnw`)
- **Node.js** 20.x LTS ou supérieur, **npm** ≥ 10
- **Angular CLI** 21 (`npm install -g @angular/cli@21`)

## Démarrage rapide

### 1. Cloner le repo

```bash
git clone https://github.com/ayoubglb/PadelManager.git
cd PadelManager
```

### 2. Lancer la base de données

```bash
docker compose up -d
```

Au premier démarrage, le script `docker/mssql-init/01-create-users.sql` est automatiquement exécuté et crée :

- la base de données `PadelManager`
- l'utilisateur `padel_liquibase` (DDL + DML)
- l'utilisateur `padel_app` (DML uniquement)

Vérifier que la base est prête :

```bash
docker compose ps
# padelmanager-mssql   Up (healthy)
```

### 3. Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
# Windows :
# .\mvnw.cmd spring-boot:run
```

Le backend démarre sur `http://localhost:8080`. Au premier démarrage, Liquibase applique les migrations de schéma sur la base.

### 4. Lancer le frontend

Dans un autre terminal :

```bash
cd frontend
npm install
npm start
```

Le frontend est accessible sur `http://localhost:4200`.

## Structure du mono-repo

```
PadelManager/
├── backend/              Application Spring Boot (Java 25, Maven)
├── frontend/             Application Angular 21
├── docker/               Scripts d'initialisation Docker
│   └── mssql-init/       Scripts SQL exécutés au 1er démarrage MSSQL
├── .github/workflows/    Workflows GitHub Actions (CI)
├── docker-compose.yml    Conteneurisation de la DB uniquement
└── README.md             Ce fichier
```

## Identifiants DB (dev local)

| Utilisateur       | Mot de passe            | Usage                             |
|-------------------|-------------------------|-----------------------------------|
| `sa`              | `Padel_SA_Pass_2026!`   | Administration (jamais l'app)     |
| `padel_liquibase` | `Padel_Liquibase_2026!` | Migrations Liquibase              |
| `padel_app`       | `Padel_App_2026!`       | Runtime application Spring Boot   |


## Commandes utiles

### Backend

```bash
./mvnw clean install       # build + tests
./mvnw spring-boot:run     # démarrage
./mvnw test                # tests unitaires
./mvnw verify              # tests + rapport de couverture Jacoco
```

### Frontend

```bash
npm start                  # dev server sur :4200
npm run build              # build de production
npm test                   # tests unitaires (Vitest)
ng e2e                     # tests end-to-end (Cypress, GUI)
```

### Base de données

```bash
docker compose up -d                        # démarrer
docker compose down                         # arrêter (garde les données)
docker compose down -v                      # arrêter + supprimer les données
docker compose logs -f mssql                # voir les logs
docker compose exec mssql /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Padel_SA_Pass_2026!' -C -N    # shell SQL
```

## URLs utiles (après lancement)

| URL | Description |
|-----|-------------|
| http://localhost:4200 | Frontend Angular |
| http://localhost:8080 | Backend Spring Boot |
| http://localhost:8080/swagger-ui/index.html | Documentation Swagger de l'API REST |
| http://localhost:8080/v3/api-docs | Spec OpenAPI JSON brute |
| http://localhost:8080/actuator/health | Healthcheck backend |