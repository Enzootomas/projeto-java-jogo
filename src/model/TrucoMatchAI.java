package model;

import java.util.List;

/**
 * Estrategia honesta de decisao para os Bots do Truco Paulista (Individual ou Duplas).
 *
 * <p>Recebe estritamente a propria mao do Bot, a carta Vira publica, as cartas ja jogadas
 * na vaza atual e o placar publico — nunca tem acesso as cartas ocultas de nenhum outro lugar.</p>
 */
public interface TrucoMatchAI {

    /**
     * Escolhe uma carta da propria mao para jogar na vaza atual.
     */
    Card escolherCarta(List<Card> maoPropria,
                       Card vira,
                       List<TrucoPlayedCard> jogadasNaVazaAtual,
                       TrucoTeam minhaEquipe);

    /**
     * Decide se o Bot deseja pedir Truco (3 pontos) ou aumentar a aposta antes de jogar sua carta.
     */
    boolean decidirPedirTruco(List<Card> maoPropria,
                              Card vira,
                              TrucoTeam minhaEquipe,
                              int valorMaoAtual,
                              int valorProposto,
                              int vazasVencidasMinhaEquipe,
                              int vazasVencidasOponente);

    /**
     * Decide a resposta do Bot diante de um pedido de Truco/Seis/Nove/Doze (ou Mao de 11).
     * Retorna {@link TrucoBetAction#ACEITAR}, {@link TrucoBetAction#CORRER} ou {@link TrucoBetAction#AUMENTAR}.
     */
    TrucoBetAction responderPedidoTruco(List<Card> maoPropria,
                                        Card vira,
                                        int valorProposto,
                                        boolean podeAumentar,
                                        int vazasVencidasMinhaEquipe,
                                        int vazasVencidasOponente);

    /**
     * Implementacao tatica padrao para os Bots (1v1 e 2v2).
     */
    class DefaultAI implements TrucoMatchAI {

        @Override
        public Card escolherCarta(List<Card> maoPropria,
                                  Card vira,
                                  List<TrucoPlayedCard> jogadasNaVazaAtual,
                                  TrucoTeam minhaEquipe) {
            if (maoPropria == null || maoPropria.isEmpty()) {
                throw new IllegalArgumentException("Mao do Bot vazia.");
            }
            TrucoCardComparator comp = new TrucoCardComparator(vira);

            // Se abre a vaza: joga a carta mais fraca para sondar (ou intermediaria se tiver 3)
            if (jogadasNaVazaAtual == null || jogadasNaVazaAtual.isEmpty()) {
                return menorCarta(maoPropria, comp);
            }

            // Encontra a carta mais forte ja jogada na mesa e de qual equipe ela e
            TrucoPlayedCard lider = jogadasNaVazaAtual.get(0);
            boolean empateAtual = false;
            for (int i = 1; i < jogadasNaVazaAtual.size(); i++) {
                TrucoPlayedCard j = jogadasNaVazaAtual.get(i);
                int d = comp.compare(j.getCarta(), lider.getCarta());
                if (d > 0) {
                    lider = j;
                    empateAtual = false;
                } else if (d == 0 && j.getEquipe() != lider.getEquipe()) {
                    empateAtual = true;
                }
            }

            // Em Duplas: se o parceiro ja esta vencendo a vaza com carta forte (2, 3 ou manilha), economiza
            if (!empateAtual && lider.getEquipe() == minhaEquipe) {
                if (TrucoRules.isManilha(lider.getCarta(), vira)
                        || lider.getCarta().getRank() == Rank.TRES
                        || lider.getCarta().getRank() == Rank.DOIS) {
                    return menorCarta(maoPropria, comp);
                }
            }

            // Procura a menor carta capaz de superar a carta lider atual
            Card menorVencedora = null;
            Card empatadora = null;
            for (Card c : maoPropria) {
                int diff = comp.compare(c, lider.getCarta());
                if (diff > 0) {
                    if (menorVencedora == null || comp.compare(c, menorVencedora) < 0) {
                        menorVencedora = c;
                    }
                } else if (diff == 0 && empatadora == null) {
                    empatadora = c;
                }
            }

            if (menorVencedora != null) {
                return menorVencedora;
            }
            if (empatadora != null) {
                return empatadora;
            }
            return menorCarta(maoPropria, comp);
        }

        @Override
        public boolean decidirPedirTruco(List<Card> maoPropria,
                                         Card vira,
                                         TrucoTeam minhaEquipe,
                                         int valorMaoAtual,
                                         int valorProposto,
                                         int vazasVencidasMinhaEquipe,
                                         int vazasVencidasOponente) {
            int forca = avaliarForcaMao(maoPropria, vira);
            if (vazasVencidasMinhaEquipe >= 1 && forca >= 22) {
                return true;
            }
            return forca >= 34;
        }

        @Override
        public TrucoBetAction responderPedidoTruco(List<Card> maoPropria,
                                                   Card vira,
                                                   int valorProposto,
                                                   boolean podeAumentar,
                                                   int vazasVencidasMinhaEquipe,
                                                   int vazasVencidasOponente) {
            int forca = avaliarForcaMao(maoPropria, vira);
            if (vazasVencidasMinhaEquipe >= 1) {
                forca += 10;
            }
            if (vazasVencidasOponente >= 1) {
                forca -= 6;
            }

            if (podeAumentar && forca >= 38 && valorProposto < 12) {
                return TrucoBetAction.AUMENTAR;
            }
            if (forca >= 18) {
                return TrucoBetAction.ACEITAR;
            }
            return TrucoBetAction.CORRER;
        }

        private Card menorCarta(List<Card> cartas, TrucoCardComparator comp) {
            Card menor = cartas.get(0);
            for (int i = 1; i < cartas.size(); i++) {
                if (comp.compare(cartas.get(i), menor) < 0) {
                    menor = cartas.get(i);
                }
            }
            return menor;
        }

        private int avaliarForcaMao(List<Card> mao, Card vira) {
            int soma = 0;
            for (Card c : mao) {
                if (TrucoRules.isManilha(c, vira)) {
                    soma += 18 + TrucoRules.getForcaNaipeManilha(c.getSuit()) * 2;
                } else {
                    soma += TrucoRules.getForcaCartaComum(c.getRank());
                }
            }
            return soma;
        }
    }
}
