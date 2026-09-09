# CA-02 — recuperação de senha por código no e-mail

Arquivos da funcionalidade usam os nomes `CodigoEmail`, `RecuperacaoEmailCodigo`,
`EnvioCodigoEmail` e `RecuperacaoUsuario`. O cadastro da CA-01 não foi alterado.

## Configuração local

Copie `application-secrets.example.properties` para
`application-secrets.properties`. O arquivo real está no `.gitignore`.

Execute a API com `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
O perfil local usa H2. Para SMTP real, defina `EMAIL_MODE=smtp` no arquivo de
segredos; sem isso, o código aparece somente no terminal.

Endpoints:

- `POST /auth/email/solicitar`
- `POST /auth/email/validar-codigo`
- `POST /auth/email/redefinir-senha`

O código expira em 10 minutos, tem limite de tentativas, é armazenado somente
como hash e é invalidado após a alteração. A senha também é armazenada como hash.
