package br.edu.uniesp.lastro.compartilhado.erro;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    static final String PROPRIEDADE_ERROS = "erros";

    private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @ExceptionHandler(DadosInvalidosException.class)
    ProblemDetail tratarDadosInvalidos(DadosInvalidosException excecao) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", excecao.getErros());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail tratarConflito(ConflitoException excecao) {
        return problema(HttpStatus.CONFLICT, excecao.getMessage(), List.of(excecao.getErro()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException excecao) {
        return problema(HttpStatus.NOT_FOUND, excecao.getMessage(), List.of());
    }

    @ExceptionHandler(MultipartException.class)
    ProblemDetail tratarMultipartInvalido(MultipartException excecao) {
        return problema(
                HttpStatus.BAD_REQUEST,
                "Envio de arquivo inválido",
                List.of(new ErroCampo("arquivo", "Envie o arquivo como multipart/form-data")));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail tratarErroInesperado(Exception excecao) {
        LOG.error("Erro inesperado ao processar a requisição", excecao);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException excecao, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErroCampo erro = new ErroCampo(campoComValorInvalido(excecao), "Valor em formato inválido");
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", List.of(erro)));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException excecao, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErroCampo erro = new ErroCampo("arquivo", "O arquivo excede o tamanho máximo permitido");
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Arquivo muito grande", List.of(erro)));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException excecao, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErroCampo erro = new ErroCampo(excecao.getPropertyName(), "Valor em formato inválido");
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido", List.of(erro)));
    }

    private static String campoComValorInvalido(HttpMessageNotReadableException excecao) {
        if (excecao.getCause() instanceof JacksonException causa
                && !causa.getPath().isEmpty()) {
            return causa.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(nome -> nome != null)
                    .collect(Collectors.joining("."));
        }
        return null;
    }

    private static ProblemDetail problema(HttpStatus status, String detalhe, List<ErroCampo> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setProperty(PROPRIEDADE_ERROS, erros);
        return problema;
    }
}
