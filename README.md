# CA-06 API

API REST construída com Java 21, Spring Boot e Maven.

## Banco de dados

A API aponta por padrão para o MySQL local no schema `capricha`:

```text
jdbc:mysql://127.0.0.1:3306/capricha
```

Crie o schema, se necessário:

```sql
CREATE DATABASE capricha CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Configure a credencial antes de iniciar (a senha não é versionada):

```powershell
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "sua-senha"
mvn spring-boot:run
```

Também é possível alterar host, porta e schema com `MYSQL_HOST`, `MYSQL_PORT` e `MYSQL_DATABASE`.

## Executar

```powershell
mvn spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

No Windows, tambem pode ser usado `mvn.cmd spring-boot:run` ou o JAR executavel
gerado pelo Maven:

```powershell
mvn.cmd spring-boot:run
# ou, depois de executar mvn package:
java -jar target\ca-06-api-0.0.1-SNAPSHOT.jar
```

Nao execute `java` diretamente sobre `Ca06ApiApplication.class`, pois esse
comando nao carrega automaticamente as dependencias do Spring Boot.

## Endpoint inicial

`GET /api/health` retorna o estado básico da API.

## Testar e empacotar

```powershell
mvn test
mvn package
```

## Check-in por QR Code

Com a API em execucao, o fluxo usa os endpoints abaixo:

- `POST /api/auth/login` para obter o token Bearer;
- `GET /api/events` para listar eventos;
- `GET /api/events/{id}/qr-code` para organizador ou administrador gerar o QR Code;
- `POST /api/events/{id}/check-in` com `{ "qrToken": "..." }` para registrar a entrada;
- `GET /api/events/{id}/participants` para organizador ou administrador consultar participantes.

Em um banco vazio, a aplicacao cria dados de demonstracao quando
`app.seed-demo-data=true`:

```text
participante@capricha.local / demo123
organizador@capricha.local / demo123
```

As migracoes Flyway criam as tabelas `users`, `events` e `event_checkin` e
garantem que o mesmo participante nao faca check-in duas vezes no mesmo evento.
