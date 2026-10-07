package com.pista.estoque.spring.messaging;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MensagemJson {

    public record Recebida(String eventoId, String tipo, JsonNode dados) {
    }

    public record Pronta(String eventoId, String json) {
    }

    private final JsonMapper mapper;

    public MensagemJson(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public Pronta montar(String tipo, Object dados) {
        String eventoId = UUID.randomUUID().toString();
        var envelope = mapper.createObjectNode();
        envelope.put("eventoId", eventoId);
        envelope.put("tipo", tipo);
        envelope.put("ocorridoEm", Instant.now().toString());
        envelope.set("dados", mapper.valueToTree(dados));
        return new Pronta(eventoId, mapper.writeValueAsString(envelope));
    }

    public Recebida ler(String json) {
        JsonNode raiz = mapper.readTree(json);
        return new Recebida(raiz.get("eventoId").asString(), raiz.get("tipo").asString(), raiz.get("dados"));
    }

    public <T> T dados(Recebida mensagem, Class<T> tipo) {
        return mapper.treeToValue(mensagem.dados(), tipo);
    }
}
