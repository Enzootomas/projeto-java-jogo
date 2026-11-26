package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa uma vaza imutavel de Truco Paulista suportando 2 participantes (Individual)
 * ou 4 participantes (Duplas), com resolucao por equipe e deteccao de empate (canga).
 */
public final class TrucoTableTrick {

    private final int numeroVaza;
    private final int totalParticipantes;
    private final int primeiroSeatIndex;
    private final Card vira;
    private final List<TrucoPlayedCard> jogadas;
    private final boolean completa;
    private final TrucoTeam equipeVencedora;
    private final int seatVencedor;

    public TrucoTableTrick(int numeroVaza, int totalParticipantes, int primeiroSeatIndex, Card vira) {
        this(numeroVaza, totalParticipantes, primeiroSeatIndex, vira, List.of());
    }

    private TrucoTableTrick(int numeroVaza,
                            int totalParticipantes,
                            int primeiroSeatIndex,
                            Card vira,
                            List<TrucoPlayedCard> jogadas) {
        this.numeroVaza = numeroVaza;
        this.totalParticipantes = totalParticipantes;
        this.primeiroSeatIndex = primeiroSeatIndex;
        this.vira = Objects.requireNonNull(vira, "Vira nao pode ser nula.");
        this.jogadas = List.copyOf(jogadas);
        this.completa = (this.jogadas.size() == totalParticipantes);

        if (this.completa) {
            TrucoCardComparator comp = new TrucoCardComparator(vira);
            TrucoPlayedCard melhor = this.jogadas.get(0);
            boolean empateEntreEquipes = false;

            for (int i = 1; i < this.jogadas.size(); i++) {
                TrucoPlayedCard atual = this.jogadas.get(i);
                int diff = comp.compare(atual.getCarta(), melhor.getCarta());
                if (diff > 0) {
                    melhor = atual;
                    empateEntreEquipes = false;
                } else if (diff == 0 && atual.getEquipe() != melhor.getEquipe()) {
                    empateEntreEquipes = true;
                }
            }

            if (empateEntreEquipes) {
                this.equipeVencedora = null;
                this.seatVencedor = -1;
            } else {
                this.equipeVencedora = melhor.getEquipe();
                this.seatVencedor = melhor.getSeatIndex();
            }
        } else {
            this.equipeVencedora = null;
            this.seatVencedor = -1;
        }
    }

    /**
     * Retorna nova instancia imutavel desta vaza com a jogada adicional registrada.
     */
    public TrucoTableTrick comJogada(TrucoPlayedCard jogada) {
        if (completa) {
            throw new IllegalStateException("Vaza ja esta completa.");
        }
        List<TrucoPlayedCard> novaLista = new ArrayList<>(jogadas.size() + 1);
        novaLista.addAll(jogadas);
        novaLista.add(Objects.requireNonNull(jogada, "Jogada nao pode ser nula."));
        return new TrucoTableTrick(numeroVaza, totalParticipantes, primeiroSeatIndex, vira, novaLista);
    }

    public int getNumeroVaza() {
        return numeroVaza;
    }

    public int getPrimeiroSeatIndex() {
        return primeiroSeatIndex;
    }

    public List<TrucoPlayedCard> getJogadas() {
        return jogadas;
    }

    public boolean isCompleta() {
        return completa;
    }

    public boolean isEmpate() {
        return completa && equipeVencedora == null;
    }

    public TrucoTeam getEquipeVencedora() {
        return equipeVencedora;
    }

    public int getSeatVencedor() {
        return seatVencedor;
    }

    public String getResumoResultado() {
        if (!completa) {
            return "Vaza " + numeroVaza + " em andamento";
        }
        if (isEmpate()) {
            return "Vaza " + numeroVaza + ": Empate (Canga)!";
        }
        return "Vaza " + numeroVaza + ": Vitória — " + equipeVencedora.getNome();
    }
}
