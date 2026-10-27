.PHONY: start dev debug up down build restart logs logs-db ps clean info test test-e2e

APP_PORT   = 8082
DB_PORT    = 5432
DEBUG_PORT = 5006

ifeq ($(OS),Windows_NT)
	MVN := .\mvnw.cmd
else
	MVN := ./mvnw
endif

info:
	@echo ""
	@echo "  ========================================================"
	@echo "  Aplicacao : http://localhost:$(APP_PORT)"
	@echo "  Swagger   : http://localhost:$(APP_PORT)/swagger-ui.html"
	@echo "  Banco     : localhost:$(DB_PORT) (billing_db / postgres)"
	@echo "  ========================================================"
	@echo ""

info-dev:
	@echo ""
	@echo "  Banco PostgreSQL pronto em localhost:$(DB_PORT) (billing_db / postgres)"
	@echo ""

start:
	docker compose down -v --rmi local
	@$(MAKE) info
	docker compose up --build

dev:
	@echo "Subindo banco de dados PostgreSQL e aguardando ficar pronto..."
	docker compose down -v --rmi local
	docker compose up -d billing-db
	@until docker exec billing-postgres pg_isready -U postgres > /dev/null 2>&1; do sleep 1; done
	@$(MAKE) info-dev
	@echo "Banco pronto! Rode a aplicacao via IDE ou com: $(MVN) spring-boot:run"

debug:
	@echo "Subindo banco de dados PostgreSQL e aguardando ficar pronto..."
	docker compose down -v --rmi local
	docker compose up -d billing-db
	@until docker exec billing-postgres pg_isready -U postgres > /dev/null 2>&1; do sleep 1; done
	@echo ""
	@echo "  Aplicacao : http://localhost:$(APP_PORT)"
	@echo "  Swagger   : http://localhost:$(APP_PORT)/swagger-ui.html"
	@echo "  Banco     : localhost:$(DB_PORT) (billing_db / postgres)"
	@echo "  Debug JVM : localhost:$(DEBUG_PORT) (Remote JVM Debug / Attach na IDE)"
	@echo ""
	$(MVN) spring-boot:run \
		-Dspring-boot.run.profiles=default \
		-Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$(DEBUG_PORT)"

up:
	docker compose up -d

down:
	docker compose down -v

build:
	$(MVN) clean package -DskipTests

restart:
	docker compose restart

logs:
	docker compose logs -f billing-app

logs-db:
	docker compose logs -f billing-db

ps:
	docker compose ps

clean:
	$(MVN) clean
	docker compose down -v --rmi local

test:
	$(MVN) clean test

test-e2e:
	npx @usebruno/cli run src/test/bruno/caminho-feliz --env local
