package br.com.artur.catalogofilmes.exception;

public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
