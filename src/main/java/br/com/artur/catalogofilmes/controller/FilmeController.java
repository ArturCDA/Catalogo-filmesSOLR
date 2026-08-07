package br.com.artur.catalogofilmes.controller;

import br.com.artur.catalogofilmes.model.Filme;
import br.com.artur.catalogofilmes.service.FilmeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeService service;

    public FilmeController(FilmeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody Filme filme) {

        try {
            service.salvar(filme);
            return ResponseEntity.ok("Filme cadastrado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erro ao cadastrar: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Filme> listar() throws Exception {
    return service.listar();
}

}