package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa um assento (participante) na mesa de Truco Paulista (Individual ou Duplas).
 * As cartas privadas dos Bots permanecem encapsuladas e jamais sao expostas a interface.
 */
public class TrucoSeat {

    private final int seatIndex;
    private final String nome;
    private final TrucoTeam equipe;
    private final boolean humano;
    private final List<Card> mao;

    public TrucoSeat(int seatIndex, String nome, TrucoTeam equipe, boolean humano) {
        this.seatIndex = seatIndex;
        this.nome = Objects.requireNonNull(nome, "Nome do assento nao pode ser nulo.");
        this.equipe = Objects.requireNonNull(equipe, "Equipe nao pode ser nula.");
        this.humano = humano;
        this.mao = new ArrayList<>(3);
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public String getNome() {
        return nome;
    }

    public TrucoTeam getEquipe() {
        return equipe;
    }

    public boolean isHumano() {
        return humano;
    }

    public int getQuantidadeCartas() {
        return mao.size();
    }

    void definirMao(List<Card> cartas) {
        limparComLacoReverso();
        for (Card c : cartas) {
            this.mao.add(Objects.requireNonNull(c));
        }
    }

    boolean contemCarta(Card carta) {
        return mao.contains(carta);
    }

    boolean removerCarta(Card carta) {
        for (int i = mao.size() - 1; i >= 0; i--) {
            if (mao.get(i).equals(carta)) {
                mao.remove(i);
                return true;
            }
        }
        return false;
    }

    void limparComLacoReverso() {
        for (int i = mao.size() - 1; i >= 0; i--) {
            mao.remove(i);
        }
    }

    List<Card> getMaoInternaSnapshot() {
        return List.copyOf(mao);
    }
}
