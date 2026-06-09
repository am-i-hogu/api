# am-i-hogu API

## 로컬 실행

### 1. 환경변수 준비

팀 Notion을 참고해서 `.env` 또는 `.env.local`을 준비한다.

### 2. MySQL 컨테이너 실행

서버 실행만으로 DB 컨테이너가 자동 실행되지는 않는다. 먼저 Docker Compose로 MySQL을 띄운다.

```bash
docker compose up -d db
```

상태 확인:

```bash
docker compose ps
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

기본 프로필은 `local`이며, 서버는 `http://localhost:8080`에서 실행된다.

애플리케이션 시작 시 Flyway가 DB 마이그레이션을 자동으로 실행한다.

### 4. 종료

서버는 실행 중인 터미널에서 `Ctrl + C`로 종료한다.

DB 컨테이너 종료:

```bash
docker compose down
```
