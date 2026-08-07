package br.com.artur.catalogofilmes.service;

import br.com.artur.catalogofilmes.model.Filme;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import java.util.List;

@Service
public class FilmeService {

    private final SolrClient solrClient;

    @Value("${solr.collection}")
    private String collection;

    public FilmeService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void salvar(Filme filme) throws Exception {

    SolrInputDocument documento = new SolrInputDocument();

    documento.addField("id", filme.getId());
    documento.addField("titulo", filme.getTitulo());
    documento.addField("diretor", filme.getDiretor());
    documento.addField("genero", filme.getGenero());
    documento.addField("ano", filme.getAno());
    documento.addField("descricao", filme.getDescricao());

    solrClient.add(collection, documento);
    solrClient.commit(collection);
}

    public List<Filme> listar() throws Exception {

    SolrQuery query = new SolrQuery("*:*");

    QueryResponse response = solrClient.query(collection, query);

    return response.getBeans(Filme.class);
}


}