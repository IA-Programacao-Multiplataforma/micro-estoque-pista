# Estoque (porta 8082)

Dono dos **eventos, sessões (datas) e setores** e da **reserva de ingressos**. Spring Boot, MongoDB (banco `pm_estoque`), RabbitMQ. Camadas: `domain` (regras em Java puro, portas) e `spring` (controllers, adapters, repositórios Mongo, mensageria).

## Modelo
`Evento` → `Sessao` (data, início da venda) → `Setor` (nome, preço, capacidade, disponível, reservado, vendido). O status da sessão é calculado: `ESTREIA` (venda ainda não abriu), `DISPONIVEL` ou `ESGOTADO`.

## Endpoints
Pelo gateway o prefixo é `/estoque`.

| Método e caminho | O que faz | Acesso |
|---|---|---|
| `GET /eventos`, `GET /eventos/{id}` | Vitrine e detalhes (com sessões e setores) | público |
| `GET /sessoes/{sessaoId}` | Disponibilidade por setor | público |
| `POST /eventos` | Cria evento com sessões e setores | ADMIN |
| `POST /eventos/{id}/sessoes` | Acrescenta uma data | ADMIN |
| `PUT /eventos/{id}/sessoes/{sessaoId}/setores/{setorId}` | Ajusta capacidade e/ou preço | ADMIN |
| `DELETE /eventos/{id}` | Exclui (recusa se há ingressos reservados ou vendidos) | ADMIN |
| `POST /reservas`, `GET /reservas/{id}`, `POST /reservas/{id}/liberar` | Reserva (sacola) | **interno**: só a fila chama, direto na 8082; o gateway bloqueia |

## Concorrência
A reserva é uma **operação atômica** no Mongo (`updateFirst` com `elemMatch` e `arrayFilters`): se há ingresso, decrementa `disponivel` e incrementa `reservado` num passo só, então duas pessoas nunca levam o último ingresso. Uma tarefa (a cada 10 s) devolve ao estoque as reservas vencidas. Reservar de novo substitui a sacola anterior.

## Mensageria (RabbitMQ)
Consome `reserva.confirmar` (fila `estoque.reserva-confirmar`): transforma a reserva em venda, de forma **idempotente** e dentro de uma transação, e responde com `reserva.confirmada` ou `reserva.confirmacao-falhou`. A publicação usa **Transactional Outbox**: a mensagem é gravada no Mongo na mesma transação e um publicador (a cada 1 s) a envia ao broker. Falhou 3 vezes, vai para a fila morta (`.dlq`).

## Configuração
| Variável | Para quê |
|---|---|
| `MONGO_URI` | MongoDB Atlas |
| `RABBITMQ_URI` | CloudAMQP |

No `application.yaml`: `estoque.reserva.max-ingressos-por-compra` (6) e `estoque.reserva.intervalo-expiracao-ms` (10 s).

## Como rodar e testar
```bash
./mvnw spring-boot:run
./mvnw test      # 30 testes, sem banco nem RabbitMQ
```
