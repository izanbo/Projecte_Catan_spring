package catan.catanSpring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import catan.catanSpring.dto.NovaPartidaPeticio;
import catan.catanSpring.services.PartidaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/partida")
public class PartidaController {

    @Autowired
    private PartidaService partidaService;

    @PostMapping
    public ResponseEntity<?> iniciarPartida(@Valid @RequestBody NovaPartidaPeticio peticio) {
        return partidaService.iniciarPartida(peticio);
    }

    @GetMapping("/{idPartida}/tauler")
    public ResponseEntity<?> obtenirTauler(@PathVariable int idPartida) {
        return partidaService.obtenirTauler(idPartida);
    }
}

