package br.com.artur.catalogofilmes.service;

import br.com.artur.catalogofilmes.dto.FilmeDTO;
import br.com.artur.catalogofilmes.exception.ServicoIndisponivelException;
import br.com.artur.catalogofilmes.model.Filme;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FilmeService {

    private static final int LIMITE_MAXIMO = 100;

    private final SolrClient solrClient;

    @Value("${solr.collection}")
    private String collection;

    public FilmeService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void salvar(FilmeDTO dto) {
        Filme filme = converter(dto);

        if (filme.getId() == null) {
            filme.setId(UUID.randomUUID().toString());
        }

        SolrInputDocument documento = new SolrInputDocument();
        adicionarCampo(documento, "id", filme.getId());
        adicionarCampo(documento, "titulo", filme.getTitulo());
        adicionarCampo(documento, "diretor", filme.getDiretor());
        adicionarCampo(documento, "genero", filme.getGenero());
        adicionarCampo(documento, "ano", filme.getAno());
        adicionarCampo(documento, "sinopse", filme.getSinopse());
        adicionarCampo(documento, "elenco", filme.getElenco());
        adicionarCampo(documento, "poster", filme.getPoster());
        adicionarCampo(documento, "nota", filme.getNota());

        try {
            solrClient.add(collection, documento);
            solrClient.commit(collection);
        } catch (SolrServerException | SolrException | IOException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível cadastrar o filme: o serviço de busca (Solr) está indisponível.", e);
        }
    }

    public void salvarEmLote(List<FilmeDTO> dtos) {
        for (FilmeDTO dto : dtos) {
            Filme filme = converter(dto);

            if (filme.getId() == null) {
                filme.setId(UUID.randomUUID().toString());
            }
            SolrInputDocument documento = new SolrInputDocument();
            adicionarCampo(documento, "id", filme.getId());
            adicionarCampo(documento, "titulo", filme.getTitulo());
            adicionarCampo(documento, "diretor", filme.getDiretor());
            adicionarCampo(documento, "genero", filme.getGenero());
            adicionarCampo(documento, "ano", filme.getAno());
            adicionarCampo(documento, "sinopse", filme.getSinopse());
            adicionarCampo(documento, "elenco", filme.getElenco());
            adicionarCampo(documento, "poster", filme.getPoster());
            adicionarCampo(documento, "nota", filme.getNota());
            
            try {
                solrClient.add(collection, documento);
            } catch (SolrServerException | IOException e) {
                throw new ServicoIndisponivelException("Erro ao adicionar filme no lote", e);
            }
        }
    
        try {
            solrClient.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new ServicoIndisponivelException("Erro ao commitar lote no Solr", e);
        }
    }

    public List<Filme> listar() {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(LIMITE_MAXIMO);
        try {
            QueryResponse response = solrClient.query(collection, query);
            return converterDocumentos(response);
        } catch (SolrServerException | SolrException | IOException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível consultar os filmes: o serviço de busca (Solr) está indisponível.", e);
        }
    }

    public List<Filme> buscar(String termo) {
        if (termo == null || termo.isBlank()) {
            return listar();
        }

        String[] palavras = termo.toLowerCase(Locale.ROOT).trim().split("\\s+");

        String consulta = Arrays.stream(palavras)
                .map(palavra -> {
                    String p = ClientUtils.escapeQueryChars(palavra);
                    String queryTexto = "titulo:*" + p + "* OR diretor:*" + p + "* OR genero:*" + p + "* OR sinopse:*" + p + "* OR elenco:*" + p + "*";

                    if (p.matches("\\d+")) {
                        queryTexto += " OR ano:" + p;
                    }

                    return queryTexto;
                })
                .collect(Collectors.joining(" OR "));

        SolrQuery query = new SolrQuery(consulta);
        query.setRows(LIMITE_MAXIMO);

        try {
            QueryResponse response = solrClient.query(collection, query);
            return converterDocumentos(response);
        } catch (SolrServerException | SolrException | IOException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível buscar filmes: o serviço de busca (Solr) está indisponível.", e);
        }
    }

    private List<Filme> converterDocumentos(QueryResponse response) {
        return response.getResults().stream()
                .map(this::converterDocumento)
                .collect(Collectors.toList());
    }

    private Filme converterDocumento(SolrDocument doc) {
        Filme filme = new Filme();
        filme.setId(texto(doc.getFieldValue("id")));
        filme.setTitulo(texto(doc.getFieldValue("titulo")));
        filme.setDiretor(texto(doc.getFieldValue("diretor")));
        filme.setGenero(texto(doc.getFieldValue("genero")));
        filme.setAno(inteiro(doc.getFieldValue("ano")));
        filme.setSinopse(texto(doc.getFieldValue("sinopse")));
        filme.setElenco(texto(doc.getFieldValue("elenco")));
        filme.setPoster(texto(doc.getFieldValue("poster")));
        filme.setNota(decimal(doc.getFieldValue("nota")));
        return filme;
    }

    private Object primeiroValor(Object valor) {
        if (valor instanceof Collection<?> colecao && !colecao.isEmpty()) {
            return colecao.iterator().next();
        }
        return valor;
    }

    private String texto(Object valor) {
        Object primeiro = primeiroValor(valor);
        return primeiro != null ? primeiro.toString() : null;
    }

    private Integer inteiro(Object valor) {
        Object primeiro = primeiroValor(valor);
        return primeiro instanceof Number numero ? numero.intValue() : null;
    }

    private Double decimal(Object valor) {
        Object primeiro = primeiroValor(valor);
        return primeiro instanceof Number numero ? numero.doubleValue() : null;
    }

    private Filme converter(FilmeDTO dto) {
        Filme filme = new Filme();
        filme.setTitulo(dto.getTitulo());
        filme.setDiretor(dto.getDiretor());
        filme.setGenero(dto.getGenero());
        filme.setAno(dto.getAno());
        filme.setSinopse(dto.getSinopse());
        filme.setElenco(dto.getElenco());
        filme.setPoster(dto.getPoster());
        filme.setNota(dto.getNota());
        return filme;
    }

    private void adicionarCampo(SolrInputDocument documento, String nome, Object valor) {
        if (valor != null) {
            documento.addField(nome, valor);
        }
    }
}
