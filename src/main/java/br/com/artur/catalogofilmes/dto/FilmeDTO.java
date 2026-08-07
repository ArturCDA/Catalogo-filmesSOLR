package br.com.artur.catalogofilmes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilmeDTO {

    @NotBlank
    private String titulo;

    @NotBlank
    private String diretor;

    @NotBlank
    private String genero;

    @NotNull
    private Integer ano;

    private String sinopse;

    private String elenco;

    private String poster;

    private Double nota;
}