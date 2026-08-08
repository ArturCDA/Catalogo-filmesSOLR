package br.com.artur.catalogofilmes.model;

import org.apache.solr.client.solrj.beans.Field;

public class Filme {

    @Field
    private String id;

    @Field
    private String titulo;

    @Field
    private String diretor;

    @Field
    private String genero;

    @Field
    private Integer ano;

    @Field
    private String sinopse;

    @Field
    private String elenco;

    @Field
    private String poster;

    @Field
    private Double nota;

    public Filme() {
    }

    public Filme(String id, String titulo, String diretor, String genero, Integer ano,
                 String sinopse, String elenco, String poster, Double nota) {
        this.id = id;
        this.titulo = titulo;
        this.diretor = diretor;
        this.genero = genero;
        this.ano = ano;
        this.sinopse = sinopse;
        this.elenco = elenco;
        this.poster = poster;
        this.nota = nota;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDiretor() {
        return diretor;
    }

    public void setDiretor(String diretor) {
        this.diretor = diretor;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public String getElenco() {
        return elenco;
    }

    public void setElenco(String elenco) {
        this.elenco = elenco;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public Double getNota() {
        return nota;
    }

    public void setNota(Double nota) {
        this.nota = nota;
    }

    @Override
    public String toString() {
        return "Filme{id='" + id + "', titulo='" + titulo + "', diretor='" + diretor
                + "', genero='" + genero + "', ano=" + ano + ", nota=" + nota + "}";
    }
}
