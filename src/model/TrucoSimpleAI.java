package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Inteligência artificial simples para o adversário do Truco Paulista.
 *
 * Princípio de segurança e isolamento:
 * O bot SÓ tem acesso à sua própria mão e às informações públicas da mesa
 * (a carta vira e a carta jogada pelo adversário na vaza atual).
 * O bot NUNCA tem acesso à mão oculta do jogador humano nem às cartas restantes do monte.
 */
public class TrucoSimpleAI {

    public TrucoSimpleAI() {
    }

    /**
     * Escolhe uma carta para jogar a partir exclusivamente das cartas disponíveis na mão do Bot,
     * da carta vira pública e da carta já jogada pelo adversário na vaza (ou null se for a vez do Bot abrir).
     *
     * @param maoBot Cartas atualmente na mão do Bot (não pode ser vazia).
     * @param vira Carta vira pública da mesa.
     * @param cartaAdversarioNaMesa Carta pública jogada pelo adversário nesta vaza, ou null se o Bot abre a vaza.
     * @return A carta selecionada da mão do Bot para ser jogada.
     */
    public Card escolherCarta(List<Card> maoBot, Card vira, Card cartaAdversarioNaMesa) {
        if (maoBot == null || maoBot.isEmpty()) {
            throw new IllegalArgumentException("A mão do bot não pode ser nula ou vazia.");
        }
        if (vira == null) {
            throw new IllegalArgumentException("A carta vira pública não pode ser nula.");
        }

        // Se tem apenas 1 carta, não há escolha
        if (maoBot.size() == 1) {
            return maoBot.get(0);
        }

        TrucoCardComparator comparador = new TrucoCardComparator(vira);

        // Cenário 1: O Bot abre a vaza (primeiro a jogar)
        if (cartaAdversarioNaMesa == null) {
            // Estratégia: joga a carta mais baixa para economizar cartas altas
            Card menorCarta = maoBot.get(0);
            for (int i = 1; i < maoBot.size(); i++) {
                Card atual = maoBot.get(i);
                if (comparador.compare(atual, menorCarta) < 0) {
                    menorCarta = atual;
                }
            }
            return menorCarta;
        }

        // Cenário 2: O adversário já jogou uma carta pública na mesa
        List<Card> cartasVencedoras = new ArrayList<>();
        for (Card carta : maoBot) {
            if (comparador.compare(carta, cartaAdversarioNaMesa) > 0) {
                cartasVencedoras.add(carta);
            }
        }

        // Se o Bot possui cartas que vencem a do adversário:
        if (!cartasVencedoras.isEmpty()) {
            // Seleciona a MENOR entre as cartas vencedoras para economizar recursos
            Card menorVencedora = cartasVencedoras.get(0);
            for (int i = 1; i < cartasVencedoras.size(); i++) {
                Card atual = cartasVencedoras.get(i);
                if (comparador.compare(atual, menorVencedora) < 0) {
                    menorVencedora = atual;
                }
            }
            return menorVencedora;
        }

        // Se nenhuma carta vence a do adversário: descarta a sua menor carta (lixo)
        Card menorDescarte = maoBot.get(0);
        for (int i = 1; i < maoBot.size(); i++) {
            Card atual = maoBot.get(i);
            if (comparador.compare(atual, menorDescarte) < 0) {
                menorDescarte = atual;
            }
        }
        return menorDescarte;
    }
}
