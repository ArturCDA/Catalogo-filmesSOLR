package br.com.artur.catalogofilmes.controller;

import br.com.artur.catalogofilmes.dto.FilmeDTO;
import br.com.artur.catalogofilmes.model.Filme;
import br.com.artur.catalogofilmes.service.FilmeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/filmes")
public class FilmeController {

    private final FilmeService service;

    public FilmeController(FilmeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> cadastrar(@Valid @RequestBody FilmeDTO dto) {
        service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensagem", "Filme cadastrado com sucesso!"));
    }

    @PostMapping("/lote")
    public ResponseEntity<Map<String, String>> cadastrarEmLote(@RequestBody List<FilmeDTO> dtos) {
        service.salvarEmLote(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensagem", dtos.size() + " filmes cadastrados com sucesso!"));
    }

    @GetMapping
    public List<Filme> listar() {
        return service.listar();
    }

    @GetMapping("/buscar")
    public List<Filme> buscar(@RequestParam(value = "termo", required = false) String termo) {
        return service.buscar(termo);
    }
}
