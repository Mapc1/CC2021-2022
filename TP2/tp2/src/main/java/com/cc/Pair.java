package com.cc;

public class Pair<T, U> {
    private final T t;
    private final U u;

    /**
     * Cria um par de dados
     * 
     * @param t Primeiro elemento
     * @param u Segundo elemento
     */
    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }

    /**
     * Método que obtém o primeiro elemento do par
     * 
     * @return Primeiro elemento
     */
    public T fst() {
        return t;
    }

    /**
     * Método que obtém o segundo elemento do par
     * 
     * @return Segundo elemento
     */
    public U snd() {
        return u;
    }
}
