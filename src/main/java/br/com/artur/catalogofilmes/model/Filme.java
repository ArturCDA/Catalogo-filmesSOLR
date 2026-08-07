package br.com.artur.catalogofilmes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.solr.client.solrj.beans.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Field("descricao")
    private String descricao;

}