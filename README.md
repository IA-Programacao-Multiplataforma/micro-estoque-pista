# Estoque (porta 8082)

Dono dos **eventos, sessões (datas) e setores**, e da **quantidade de ingressos**. É o único serviço que altera o estoque. Spring Boot, MongoDB (banco `pm_estoque`), RabbitMQ.

## Modelo
`Evento` → `Sessao` (data, início da venda) → `Setor` (nome, preço, capacidade, disponível, vendido). Invariante: `capacidade = disponível + vendido`. O status da sessão é calculado: `ESTREIA` (a venda ainda não abriu), `DISPONIVEL` ou `ESGOTADO`.

## Mensageria: a venda
| | |
|---|---|
| **Recebe** | `pedido.criado` (fila `estoque.pedido-criado`): pedidoId, sessaoId e itens |
| **Envia** | `pedido.realizado` (havia ingressos, baixa feita) ou `pedido.cancelado` (com o motivo) |

Quem decide é o [`VendaService`](src/main/java/com/pista/estoque/domain/service/VendaService.java), numa **transação com lock pessimista**:

1. **trava** o evento da sessão: escreve nele primeiro, e o Mongo segura a trava até o commit (`EventoRepositoryImpl.travarParaVenda`);
2. **lê** o disponível de cada setor, já com a trava;
3. se há ingressos para todos os itens, **dá baixa** (`disponivel -= qtd`, `vendido += qtd`), faz o commit e envia `pedido.realizado`;
4. se falta em algum setor, lança `VendaRecusadaException`: **rollback** (nenhuma baixa fica) e envia `pedido.cancelado`.

Outro pedido para o mesmo evento, durante a transação, recebe `WriteConflict` e tenta de novo depois ([`MongoUnidadeDeTrabalho`](src/main/java/com/pista/estoque/spring/config/MongoUnidadeDeTrabalho.java), até 10 vezes). Resultado: nunca se vende mais do que existe no banco.

Também são recusados: sessão inexistente, venda ainda não aberta, setor inexistente e mais de 6 ingressos por compra (`estoque.max-ingressos-por-compra`).

## Endpoints (cadastro)
Pelo gateway o prefixo é `/estoque`.

| Método e caminho | O que faz | Acesso |
|---|---|---|
| `GET /eventos`, `GET /eventos/{id}` | Vitrine e detalhes (com sessões e setores) | público |
| `POST /eventos` | Cria evento com sessões e setores | ADMIN |
| `PUT /eventos/{id}` | Edita título, descrição, categoria, local, cidade e imagem | ADMIN |
| `DELETE /eventos/{id}` | Exclui (recusa se já há ingressos vendidos) | ADMIN |
| `POST /eventos/{id}/sessoes` | Acrescenta uma data | ADMIN |
| `PUT /eventos/{id}/sessoes/{sessaoId}` | Troca a data do evento e o início da venda | ADMIN |
| `DELETE /eventos/{id}/sessoes/{sessaoId}` | Exclui a data (recusa se há ingressos vendidos ou se for a única data) | ADMIN |
| `POST /eventos/{id}/sessoes/{sessaoId}/setores` | Acrescenta um tipo de ingresso (nome repetido na mesma data é recusado) | ADMIN |
| `PUT /eventos/{id}/sessoes/{sessaoId}/setores/{setorId}` | Muda nome, capacidade e/ou preço (só o que for enviado; a capacidade não pode ficar abaixo do vendido) | ADMIN |
| `DELETE /eventos/{id}/sessoes/{sessaoId}/setores/{setorId}` | Exclui o tipo de ingresso (recusa se há vendidos ou se for o único da data) | ADMIN |

Todas as alterações passam pelo [`EventoService`](src/main/java/com/pista/estoque/domain/service/EventoService.java), que roda numa transação e **trava o evento** antes de gravar (a mesma trava da venda). Assim uma edição nunca sobrescreve uma venda em andamento, e uma venda nunca acontece no meio de uma exclusão.

## Configuração
| Variável | Para quê |
|---|---|
| `MONGO_URI` | MongoDB Atlas |
| `RABBITMQ_URI` | CloudAMQP |

## Como rodar e testar
```bash
./mvnw spring-boot:run
./mvnw test      # testes de domínio (EventoTest, VendaServiceTest), sem banco nem RabbitMQ
```
