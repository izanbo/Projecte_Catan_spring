package catan.catanSpring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import catan.catanSpring.services.JugadorService;

@RestController
@RequestMapping("/jugador")
public class JugadorController {

    @Autowired
    private JugadorService jugadorService;

    @GetMapping("/{idJugador}/recursos")
    public ResponseEntity<?> obtenirRecursos(@PathVariable int idJugador) {
        return jugadorService.obtenirRecursos(idJugador);
    }
}