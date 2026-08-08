# CONTEXT — Análise Técnica do Projeto Catálogo de Filmes (Spring Boot + Apache Solr)

> **Projeto:** Catalogo-filmesSOLR — API REST de catálogo de filmes com busca full-text
> **Stack:** Java 21 · Spring Boot 4.0.8 · SolrJ 9.10.1 · Apache Solr local (porta 8983, sem Docker)
> **Propósito:** roteiro de estudos para apresentação acadêmica (teoria + demonstração no Postman)

---

# 1️⃣ Análise detalhada do código (camada por camada)

## 1.1 — `CatalogoFilmesApplication.java` (o ponto de partida)

```java
@SpringBootApplication
public class CatalogoFilmesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogoFilmesApplication.class, args);
    }
}
```

- **`@SpringBootApplication`** é uma anotação composta que traz três outras:
  - `@SpringBootConfiguration` → marca a classe como fonte de configuração;
  - `@EnableAutoConfiguration` → o Spring Boot **adivinha e configura** tudo o que estiver no classpath (Tomcat embutido, Jackson, validação, etc.);
  - `@ComponentScan` → varre o pacote `br.com.artur.catalogofilmes` e todos os subpacotes procurando beans (`@Controller`, `@Service`, `@Configuration`...).
- `SpringApplication.run(...)` sobe o Tomcat embutido na porta `8080` (definida no `application.properties`).

> 🎤 **Para a apresentação:** é aqui que você explica que o projeto **não tem servidor externo** — o Tomcat vem embutido no jar.

---

## 1.2 — `config/SolrConfig.java` (a conexão com o Solr)

```java
@Configuration
public class SolrConfig {

    @Value("${solr.url}")          // injeta o valor de application.properties
    private String solrUrl;

    @Bean
    public SolrClient solrClient() {
        return new HttpSolrClient.Builder(solrUrl).build();
    }
}
```

- **`@Configuration`** → classe de configuração; o Spring instancia um único bean por método anotado com `@Bean` (singleton — um único cliente para toda a aplicação, reaproveitado em todas as requisições).
- **`@Value("${solr.url}")`** → lê `solr.url=http://localhost:8983/solr` do `application.properties` e injeta na variável.
- **`HttpSolrClient`** → é um **cliente HTTP** (SolrJ) que fala a REST API do Solr. Ele não é mágico: ele serializa documentos, monta requisições HTTP e envia para `localhost:8983`. Ou seja, **tudo que o SolrJ faz, você consegue fazer com `curl`** — ótima frase para a apresentação.

**`application.properties`** — os 4 parâmetros do projeto:
```properties
spring.application.name=catalogo-filmes
server.port=8080                  # porta do nosso Spring Boot
solr.url=http://localhost:8983/solr   # endereço do Solr (instância local, sem Docker)
solr.collection=filmes               # collection onde os filmes vivem
```

---

## 1.3 — `controller/FilmeController.java` (a porta de entrada HTTP)

```java
@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeService service;

    public FilmeController(FilmeService service) {   // injeção por construtor
        this.service = service;
    }
```

- **`@RestController`** = `@Controller` + `@ResponseBody`. Ou seja: o retorno de cada método **não é uma view HTML**, e sim o objeto em si, que o **Jackson** serializa automaticamente para **JSON** na resposta HTTP.
- **`@RequestMapping("/filmes")`** → prefixo de URL para todos os endpoints da classe: `POST /filmes`, `GET /filmes`, `GET /filmes/buscar`.
- **Injeção por construtor** (em vez de `@Autowired` em campo) — boa prática: o campo `service` é `final`, o que garante imutabilidade, facilita testes (você pode construir o controller com um mock) e deixa as dependências explícitas.

### Endpoint 1 — Cadastro
```java
@PostMapping
public ResponseEntity<Map<String, String>> cadastrar(@Valid @RequestBody FilmeDTO dto) {
    service.salvar(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensagem", "Filme cadastrado com sucesso!"));
}
```
- **`@RequestBody FilmeDTO dto`** → o Jackson lê o corpo JSON da requisição e o desserializa na classe `FilmeDTO` (mapeia `"titulo"` → `dto.titulo`, etc.).
- **`@Valid`** → dispara a **Bean Validation**: antes de executar o método, o Hibernate Validator verifica as anotações do DTO (`@NotBlank`, `@NotNull`). Se falhar → `MethodArgumentNotValidException` → cai no `@ControllerAdvice` → **400**.
- **`ResponseEntity.status(HttpStatus.CREATED)`** → força o status **201 Created** (semântica correta para criação — em vez do 200 padrão).
- **`Map.of(...)`** → estrutura imutável que o Jackson serializa como `{"mensagem": "Filme cadastrado com sucesso!"}`.

### Endpoint 2 — Listar tudo
```java
@GetMapping
public List<Filme> listar() {
    return service.listar();
}
```
- Retorna `List<Filme>` → Jackson usa os **getters** do `Filme` e monta um array JSON.

### Endpoint 3 — Busca full-text (o coração da apresentação)
```java
@GetMapping("/buscar")
public List<Filme> buscar(@RequestParam(value = "termo", required = false) String termo) {
    return service.buscar(termo);
}
```
- **`@RequestParam(value = "termo", required = false)`** → lê o query param `?termo=Nolan` da URL. `required = false` significa que a rota **não quebra** se o parâmetro vier vazio ou ausente (defensivo — o tratamento do termo nulo fica no Service).

> 🎤 **Note a separação de responsabilidades:** o Controller é fino — só recebe HTTP, delega e devolve. Toda a lógica de negócio (conversão, sanitização, montagem da query, mapeamento) está no Service. Isso é o padrão **MVC** clássico do Spring.

---

## 1.4 — `dto/FilmeDTO.java` (contrato de entrada + validação)

```java
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

    private String sinopse;   // opcionais
    private String elenco;
    private String poster;
    private Double nota;
}
```

- **`@Data` (Lombok)** → gera getters, setters, `toString`, `equals`, `hashCode` e um construtor. Como a classe não tem campos `final`, o construtor gerado é o **sem argumentos** — requisito do Jackson para desserializar.
- **Por que um DTO separado da entidade `Filme`?** O DTO é o **contrato de entrada da API** — define o que o cliente *deve* mandar e as regras de validação. A entidade é a representação do *documento no Solr*. Separar os dois permite validar a entrada sem poluir o modelo.
- **`@NotBlank`** ≠ `@NotNull`: `@NotNull` só exige que não seja null; `@NotBlank` exige não-null, não-vazio e não-só-espaços (`" "` falha). Por isso `titulo`, `diretor`, `genero` usam `@NotBlank` e `ano` (numérico) usa `@NotNull`.
- **Mensagens em PT-BR** — o `message` é o texto que o `@ControllerAdvice` devolve no JSON de erro.

---

## 1.5 — `service/FilmeService.java` (o cérebro) — **analisado linha a linha**

### Estrutura e injeção
```java
@Service
public class FilmeService {

    private static final int LIMITE_MAXIMO = 100;   // teto de resultados (paginação defensiva)

    private final SolrClient solrClient;

    @Value("${solr.collection}")
    private String collection;                       // = "filmes"

    public FilmeService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }
```
- **`@Service`** → marca a classe como bean de camada de negócio (componente do Spring, injetável).
- **`LIMITE_MAXIMO = 100`** → constante única de teto de resultados, usada tanto no `listar` quanto no `buscar` (default do Solr é 10; sem `setRows`, a lista viria truncada silenciosamente).
- **`@Value("${solr.collection}")`** → injeta o nome da collection a partir das properties (configuração fora do código).
- O `SolrClient` chega por **construtor** (mesma boa prática do Controller).

### `salvar(FilmeDTO)` — da requisição ao documento indexado
```java
public void salvar(FilmeDTO dto) {
    Filme filme = converter(dto);                       // 1. DTO → entidade

    if (filme.getId() == null) {                        // 2. Solr exige id único por doc
        filme.setId(UUID.randomUUID().toString());
    }

    SolrInputDocument documento = new SolrInputDocument();  // 3. monta o "mapa" de campos
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
        solrClient.add(collection, documento);          // 4. envia ao Solr (POST /update)
        solrClient.commit(collection);                  // 5. torna pesquisável (commit)
    } catch (SolrServerException | SolrException | IOException e) {
        throw new ServicoIndisponivelException(
                "Não foi possível cadastrar o filme: o serviço de busca (Solr) está indisponível.", e);
    }
}
```
1. **`converter(dto)`** → transfere os dados do DTO para um `Filme` novo (cópia explícita campo a campo).
2. **UUID como id** — o Solr tem um campo `id` como **chave única obrigatória** por documento. Geramos `UUID.randomUUID()` para nunca colidir (se o cliente não mandar id).
3. **`SolrInputDocument`** — estrutura do SolrJ equivalente a um documento: um mapa `campo → valor`. Nada é enviado ainda.
4. **`add(collection, documento)`** — aqui o SolrJ entra em ação: serializa o documento (formato XML na versão 9) e faz um **POST HTTP** para `http://localhost:8983/solr/filmes/update`. O Solr recebe e **indexa**.
5. **`commit(collection)`** — o Solr é *near-real-time*: sem commit, o documento fica no buffer e **não aparece nas buscas**. O commit confirma e torna visível.
6. **Multi-catch** — `SolrServerException` (erro de comunicação SolrJ), `SolrException` (erro retornado pelo Solr) e `IOException` (falha de rede) são todos envolvidos na nossa **exceção customizada** — o cliente nunca vê o stack trace bruto.

### `listar()` — catálogo completo
```java
public List<Filme> listar() {
    SolrQuery query = new SolrQuery("*:*");      // *:* = "todos os documentos"
    query.setRows(LIMITE_MAXIMO);                // teto defensivo de 100

    try {
        QueryResponse response = solrClient.query(collection, query);   // GET /select
        return converterDocumentos(response);    // resposta Solr → List<Filme>
    } catch (SolrServerException | SolrException | IOException e) {
        throw new ServicoIndisponivelException("Não foi possível consultar os filmes: ...", e);
    }
}
```
- **`*:*`** é a sintaxe Lucene para "match-all".
- **`setRows(100)`** limita quantos documentos voltam — **paginação defensiva** contra estouro de memória se o catálogo crescer.

### `buscar(String termo)` — busca full-text (o destaque máximo)
```java
public List<Filme> buscar(String termo) {
    if (termo == null || termo.isBlank()) {      // 🛡 busca vazia → catálogo completo (sem NPE)
        return listar();
    }

    String[] palavras = termo.toLowerCase(Locale.ROOT).trim().split("\\s+");   // "Christopher Nolan" → ["christopher", "nolan"]

    String consulta = Arrays.stream(palavras)
            .map(palavra -> {
                String p = ClientUtils.escapeQueryChars(palavra);   // 🛡 sanitização
                return "titulo:*" + p + "* OR diretor:*" + p + "* OR genero:*" + p + "*";
            })
            .collect(Collectors.joining(" OR "));

    SolrQuery query = new SolrQuery(consulta);
    query.setRows(LIMITE_MAXIMO);

    try {
        QueryResponse response = solrClient.query(collection, query);
        return converterDocumentos(response);
    } catch (SolrServerException | SolrException | IOException e) {
        throw new ServicoIndisponivelException("Não foi possível buscar filmes: ...", e);
    }
}
```

Analisando cada decisão:

| Passo | Código | Por quê |
|---|---|---|
| 🛡 Guarda de entrada | `termo == null \|\| termo.isBlank()` | Se vier `?termo=` ou sem parâmetro, devolve o catálogo inteiro em vez de `NullPointerException` ou query vazia. |
| Normalização | `toLowerCase(Locale.ROOT)` | O índice `text_general` do Solr **guarda minúsculas**. Como wildcards (`*...*`) **não passam pela análise de texto**, a busca precisa ser convertida para minúsculas manualmente para casar. `Locale.ROOT` evita bugs de locale (ex.: o "I" turco). |
| Tokenização | `split("\\s+")` | Divide "Christopher Nolan" em 2 palavras — cada uma vira um bloco `OR`. Sem isso, o espaço quebraria a sintaxe da query. |
| 🛡 Sanitização | `ClientUtils.escapeQueryChars(palavra)` | Escapa os **caracteres especiais do Lucene** (`+ - && \|\| ! ( ) { } [ ] ^ " ~ * ? : \ /`), transformando-os em literais. Isso impede **Syntax Error** e até **query injection**. |
| Wildcards | `titulo:*nolan* OR diretor:*nolan* OR genero:*nolan*` | `*...*` = **correspondência parcial** (substring) nos 3 campos. É a "busca abrangente": "nolan" acha "Christopher **Nolan**" no diretor, e "mat" acharia "The **Mat**rix" no título. |
| União | `Collectors.joining(" OR ")` | Palavras múltiplas são unidas com `OR` — qualquer palavra batendo já retorna o filme. |

**Resultado para `termo=Nolan`:** a query enviada é `titulo:*nolan* OR diretor:*nolan* OR genero:*nolan*` → bate no diretor do Interestelar. ✅

### `converterDocumentos` / `converterDocumento` — o mapeamento manual (a "revanche")
```java
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
```
- `response.getResults()` devolve os documentos do Solr como `SolrDocument` — na prática, um **mapa `campo → valor`**.
- O mapeamento é **manual** porque, no modo **schemaless** (configset `_default`), o Solr devolve os campos como **`ArrayList`** (`titulo: ["Interestelar"]`). O `getBeans(Filme.class)` do SolrJ tentava injetar essa lista num campo `String` → `BindingException`. **Essa foi a saga do erro 500 que resolvemos** — e é uma excelente história técnica para a apresentação.
- Os métodos auxiliares são **null-safe** e **à prova de schema**:

```java
private Object primeiroValor(Object valor) {
    if (valor instanceof Collection<?> colecao && !colecao.isEmpty()) {   // se for lista...
        return colecao.iterator().next();                                  // ...pega o 1º elemento
    }
    return valor;                                                          // senão, devolve direto
}

private String texto(Object valor) {
    Object primeiro = primeiroValor(valor);
    return primeiro != null ? primeiro.toString() : null;
}

private Integer inteiro(Object valor) {
    Object primeiro = primeiroValor(valor);
    return primeiro instanceof Number numero ? numero.intValue() : null;   // Java 21: pattern matching
}

private Double decimal(Object valor) {
    Object primeiro = primeiroValor(valor);
    return primeiro instanceof Number numero ? numero.doubleValue() : null;
}
```
- **`instanceof Number numero`** é *pattern matching for instanceof*, recurso moderno de Java (16+) que combina verificação **e** cast em uma linha — dá para citar na apresentação como prova do Java 21.
- `primeiroValor` resolve a questão multi-valued: se o campo voltar como lista `["Interestelar"]`, usa o primeiro item; se voltar como valor simples, usa direto. **Funciona com schema fixo ou schemaless.**

### `converter(FilmeDTO)` e `adicionarCampo`
```java
private Filme converter(FilmeDTO dto) { ... }   // DTO → entidade (sem id; o id nasce no salvar)

private void adicionarCampo(SolrInputDocument documento, String nome, Object valor) {
    if (valor != null) {                         // 🛡 não envia campos null
        documento.addField(nome, valor);
    }
}
```
- `adicionarCampo` pula valores `null` — evita poluir o documento e o schema dinâmico com campos vazios.

---

## 1.6 — `model/Filme.java` (a entidade, Java puro)

```java
public class Filme {

    @Field  private String id;       // @Field do SolrJ (org.apache.solr.client.solrj.beans.Field)
    @Field  private String titulo;
    @Field  private String diretor;
    @Field  private String genero;
    @Field  private Integer ano;
    @Field  private String sinopse;
    @Field  private String elenco;
    @Field  private String poster;
    @Field  private Double nota;
    ...
}
```
- **POJO sem Lombok**: construtor vazio (exigência para binding por reflexão), construtor cheio, todos os getters/setters explícitos e `toString`. Removemos o Lombok aqui por decisão arquitetural (robustez de build e clareza).
- **`@Field`** (do SolrJ) — nas versões anteriores isso era usado pelo `DocumentObjectBinder` no `getBeans()`. Hoje, com o mapeamento manual, eles funcionam como **documentação do vínculo entre atributo e campo do Solr** — inofensivos e didáticos.

---

## 1.7 — `exception/ServicoIndisponivelException.java` (exceção de negócio)

```java
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);     // preserva a causa original (útil no log)
    }

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
```
- Estende **`RuntimeException`** de propósito: exceções unchecked **não obrigam `throws`** nas assinaturas — o código fica limpo, e o Spring MVC a captura automaticamente ao propagar até o handler.
- Dois construtores: um com causa (preserva o erro original para debug no log) e um simples.

---

## 1.8 — `exception/GlobalExceptionHandler.java` (a rede de segurança)

```java
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
```

- **`@ControllerAdvice`** → escuta **todas** as exceções lançadas por qualquer controller da aplicação e as converte em respostas HTTP formatadas. É o "interceptador global de erros".
- **`@Slf4j`** (Lombok) → injeta um logger; os stack traces vão para o **log do servidor**, nunca para o cliente.

Os 5 handlers:

| Handler | Exceção | Status | Quando acontece |
|---|---|---|---|
| `handleValidacao` | `MethodArgumentNotValidException` | **400** | O `@Valid` falhou (campo obrigatório ausente, etc.) — devolve a lista de campos com as mensagens PT-BR |
| `handleCorpoInvalido` | `HttpMessageNotReadableException` | **400** | JSON malformado (vírgula faltando, aspas erradas, tipo errado: `"ano":"abc"`) |
| `handleServicoIndisponivel` | `ServicoIndisponivelException` | **503** | **Nossa exceção** — Solr fora do ar (a estrela da apresentação) |
| `handleSolr` | `SolrServerException`/`SolrException`/`IOException` | **503** | *Safety net* — falha Solr que escapar de qualquer outra parte (ex.: criação do bean) |
| `handleGenerico` | `Exception` | **500** | Qualquer erro inesperado — **sem stack trace** no corpo |

O corpo de erro é **padronizado** pelo helper `corpo()`:
```java
private Map<String, Object> corpo(HttpStatus status, String erro, Object detalhes) {
    Map<String, Object> corpo = new LinkedHashMap<>();   // LinkedHashMap preserva a ordem
    corpo.put("status", status.value());
    corpo.put("erro", erro);
    corpo.put("detalhes", detalhes);
    return corpo;
}
```
**Contrato de erro da API** → sempre `{"status": 503, "erro": "...", "detalhes": "..."}`. Previsível e fácil de consumir no Postman.

> 🎤 **Detalhe técnico para o professor:** `SolrServerException` é *checked* (estende `Exception`), `IOException` é *checked* e `SolrException` é `RuntimeException` — o multi-catch `SolrServerException | SolrException | IOException` **compila** porque nenhuma é subtipo da outra. E o Spring escolhe o handler mais específico automaticamente (a ordem dos métodos no arquivo não importa).

---

# 2️⃣ O Fluxo da Informação (Postman → Solr → Postman)

## Fluxo A — Cadastro (POST `/filmes`)

```
Postman ──POST http://localhost:8080/filmes, body JSON──▶ Tomcat embutido
                                                             │
   1. DispatcherServlet roteia p/ @PostMapping("/filmes")     │
   2. Jackson desserializa JSON → FilmeDTO (@RequestBody)     │
   3. @Valid valida (@NotBlank/@NotNull) ──falhou?──▶ 400 JSON (Handler de validação)
                                                             ▼
   4. FilmeController.cadastrar(dto) ──▶ FilmeService.salvar(dto)
                                             │
   5. converter(dto) → Filme + UUID como id
   6. Monta SolrInputDocument (mapa campo→valor)
   7. solrClient.add() ──POST HTTP /solr/filmes/update──▶ Solr (porta 8983)
                                             │                │
                                             │     8. Solr infere schema (modo schemaless)
                                             │        e Lucene indexa os campos de texto
   9. solrClient.commit() ──POST /update?commit=true──▶      │
                                             │     9. commit torna o doc pesquisável
                                             ▼                ▼
   Controller: 201 Created {"mensagem": "Filme cadastrado com sucesso!"}  ◀── Jackson → JSON → Postman
```

## Fluxo B — Busca full-text (GET `/filmes/buscar?termo=Nolan`)

```
Postman ──GET /filmes/buscar?termo=Nolan──▶ Tomcat
  1. DispatcherServlet → @GetMapping("/buscar")
  2. Spring liga o query param "termo" ao método (required=false)
  3. FilmeController.buscar("Nolan") ──▶ FilmeService.buscar("Nolan")
                                            │
  4. termo não é nulo/vazio? Sim
  5. Normaliza: "nolan"  (toLowerCase + trim)
  6. Sanitiza: escapeQueryChars("nolan") = "nolan"  (não tinha chars especiais)
  7. Monta: titulo:*nolan* OR diretor:*nolan* OR genero:*nolan*
  8. SolrQuery + setRows(100)
  9. solrClient.query() ──GET HTTP /solr/filmes/select?q=...&rows=100&wt=json──▶ Solr
                                            │
  10. Lucene expande o wildcard *nolan* no dicionário de termos,
      pontua os docs com BM25 e devolve [Interestelar] (score alto)
                                            │
 11. SolrJ parseia a resposta JSON → QueryResponse → List<SolrDocument>
 12. converterDocumentos() → List<Filme> (mapeamento manual, 1º valor de cada campo)
                                            ▼
  Spring serializa List<Filme> com Jackson (getters) → JSON array → 200 OK → Postman
```

### Onde o Solr "automatiza" a indexação e a busca (ponto-chave para a teoria)
- **Indexação (POST):** no modo **schemaless**, quando o documento `{id, titulo, diretor, genero, ano...}` chega, o Solr **cria dinamicamente** os campos que não existem no schema. Campos de texto viram `text_general`: o **Lucene tokeniza** (quebra em palavras), **lowercase** e **elimina stopwords**, e constrói o **índice invertido** (mapa `termo → lista de documentos`). É por isso que, na busca, "Nolan" e "nolan" funcionam igual — o índice já está em minúsculas.
- **Busca (GET):** o Solr parseia a query, expande `*nolan*` no dicionário de termos, e usa o **modelo de relevância BM25** para ordenar os documentos por score. Não é `LIKE '%nolan%'` de SQL varrendo tabelas — é a busca no **índice invertido**, por isso é rápida mesmo com muitos dados.
- **Dica de professor:** se quiser, mostre o score retornado: `?fl=*,score` no Admin UI do Solr (`http://localhost:8983/solr/#/filmes/query`).

---

# 3️⃣ Pontos de Destaque para a Apresentação (boas práticas)

### ⭐ 1. Sanitização de input — `escapeQueryChars` (equivalente ao "SQL injection" do Lucene)
```java
String p = ClientUtils.escapeQueryChars(palavra);
return "titulo:*" + p + "* OR diretor:*" + p + "* OR genero:*" + p + "*";
```
**Fale isso:** como montamos a query **concatenação de strings**, um termo com `+`, `(`, `"`, `&&` etc. quebraria a sintaxe Lucene (erro 500) ou poderia **alterar a semântica da busca** (injeção de query). O `escapeQueryChars` escapa esses caracteres, tornando-os literais. É o mesmo princípio do *prepared statement* em SQL — proteção contra injeção.

### ⭐ 2. Paginação defensiva — `setRows`
```java
private static final int LIMITE_MAXIMO = 100;
...
query.setRows(LIMITE_MAXIMO);
```
**Fale isso:** o default do Solr é retornar **10** resultados (e sem `rows`, nossa lista viria truncada sem aviso). Definimos o teto em **100** — limita o payload e protege a memória caso o catálogo cresça. "Defensivo" porque o limite é **constante única** aplicada aos dois endpoints.

### ⭐ 3. Tratamento de exceções — 503 limpo, sem stack trace
```java
@ExceptionHandler(ServicoIndisponivelException.class)
public ResponseEntity<Map<String, Object>> handleServicoIndisponivel(ServicoIndisponivelException ex) {
    log.error("Serviço de busca (Solr) indisponível", ex);       // stack trace só no log
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo(
            HttpStatus.SERVICE_UNAVAILABLE, "...", detalhes(ex)));
}
```
**Fale isso:** a API **nunca** vaza stack trace para o cliente. Exceções do Solr são convertidas em `ServicoIndisponivelException` no Service (multi-catch `SolrServerException | SolrException | IOException`) e capturadas pelo `@ControllerAdvice` → **503** com JSON padronizado. O stack trace fica **no log do servidor** para debugging.

### ⭐ 4. Busca vazia defensiva
```java
if (termo == null || termo.isBlank()) {
    return listar();
}
```
**Fale isso:** `?termo=` (vazio) ou sem parâmetro não gera `NullPointerException` nem erro — devolve o catálogo completo. Tratar a entrada antes de processar é a primeira linha de defesa.

### ⭐ 5. Mapeamento manual à prova de schema (a história do `BindingException`)
```java
private Object primeiroValor(Object valor) {
    if (valor instanceof Collection<?> colecao && !colecao.isEmpty()) {
        return colecao.iterator().next();
    }
    return valor;
}
```
**Fale isso:** o Solr **schemaless** devolve campos como arrays (`titulo: ["Interestelar"]`), e o `getBeans()` do SolrJ quebrava ao injetar lista em campo `String`. O mapeamento manual com `primeiroValor()` funciona com **qualquer** configuração de schema. Mostra domínio real do funcionamento interno do Solr.

### ⭐ 6. Outras decisões que merecem menção
- **UUID como id** → obrigatoriedade de chave única no Solr.
- **`commit` explícito** → conceito de *near-real-time*: sem commit o doc não aparece na busca.
- **`adicionarCampo` pulando null** → não polui o schema dinâmico.
- **`Locale.ROOT`** no `toLowerCase` → robustez de localização.
- **DTO + `@Valid` separado da entidade** → contrato de entrada claro; validação declarativa.
- **Injeção por construtor + `final`** → testabilidade e imutabilidade.
- **`@RestController` / `@Service` / `@ControllerAdvice`** → arquitetura em camadas (Controller → Service → Solr).

---

# 🎁 Bônus — Perguntas prováveis do professor (e respostas de 1 linha)

| Pergunta | Resposta pronta |
|---|---|
| "Por que a busca é case-insensitive?" | O índice `text_general` guarda minúsculas e wildcards não passam pela análise de texto, então normalizamos o termo com `toLowerCase(Locale.ROOT)` antes de montar a query. |
| "O que é o modo schemaless?" | O Solr infere o schema automaticamente quando o documento chega — por isso os campos voltam como arrays e precisamos do mapeamento manual com `primeiroValor()`. |
| "Qual a diferença entre `add` e `commit`?" | `add` indexa no buffer; `commit` torna visível na busca (near-real-time). Sem commit, o Postman cadastra mas a busca não encontra. |
| "Como o SolrJ se conecta ao Solr?" | `HttpSolrClient` é um cliente HTTP que fala a REST API do Solr (porta 8983). Tudo que ele faz dá para fazer com `curl`. |
| "Por que 503 e não 500?" | 503 = *Service Unavailable*: o erro não é do nosso código, é do serviço externo (Solr). 500 seria semanticamente errado. |
| "Como você evita que o usuário quebre a query?" | `ClientUtils.escapeQueryChars()` escapa os caracteres especiais do Lucene — mesma lógica anti-injeção do SQL. |
| "Por que não usar `getBeans()`?" | Porque o schemaless devolve listas e o binder lança `BindingException` ao injetar em campo `String`. O mapeamento manual resolve e independe do schema. |
| "Por que o `@Field` no Filme se o mapeamento é manual?" | Ficaram como documentação do vínculo campo↔atributo; o SolrJ também ainda os usa se algum dia o `getBeans()` for retomado. |

---

💡 **Última dica de mentor:** antes de dormir, rode uma vez o fluxo inteiro no Postman (cadastrar → buscar → listar) e depois **pare o Solr** e faça `GET /filmes/buscar?termo=Nolan` — veja o JSON 503 limpo na tela. Esse é o momento de ouro da demonstração: mostra que a aplicação **não cai** quando a dependência cai.
