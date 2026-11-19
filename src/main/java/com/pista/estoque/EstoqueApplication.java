package com.pista.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import com.pista.estoque.domain.evento.PedidoCancelado;
import com.pista.estoque.domain.evento.PedidoCriado;
import com.pista.estoque.domain.evento.PedidoRealizado;
import com.pista.estoque.spring.messaging.MensagemJson;

@SpringBootApplication
// As mensagens do RabbitMQ viram JSON por reflexão (Jackson). No executável nativo (GraalVM) os records
// precisam ser registrados antes, senão dá "Record components not available for record class ..."
@RegisterReflectionForBinding({
		PedidoCriado.class, PedidoCriado.Item.class,
		PedidoRealizado.class, PedidoRealizado.Item.class,
		PedidoCancelado.class,
		MensagemJson.Recebida.class, MensagemJson.Pronta.class
})
public class EstoqueApplication {

	public static void main(String[] args) {
		SpringApplication.run(EstoqueApplication.class, args);
	}

}
