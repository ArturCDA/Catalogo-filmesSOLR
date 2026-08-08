package br.com.artur.catalogofilmes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilmeDTO {

    @NotBlank(message = "O campo 'titulo' é obrigatório")
    private String titulo;

    @NotBlank(message = "O campo 'diretor' é obrigatório")
    private String diretor;

    @NotBlank(message = "O campo 'genero' é obrigatório")
    private String genero;

    @NotNull(message = "O campo 'ano' é obrigatório")
    private Integer ano;

    private String sinopse;

    private String elenco;

    private String poster;

    private Double nota;
}
