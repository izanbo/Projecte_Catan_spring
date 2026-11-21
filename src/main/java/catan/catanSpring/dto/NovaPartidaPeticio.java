package catan.catanSpring.dto;

import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovaPartidaPeticio(
        @NotNull(message = "La llista de noms és obligatòria")
        @Size(min = 2, max = 4, message = "Hi ha d'haver entre 2 i 4 jugadors")
        List<@NotBlank(message = "El nom del jugador no pot estar buit") String> nomsJugadors,
        @Min(value = 1, message = "Els punts per guanyar han de ser com a mínim 1")
        Integer puntsPerGuanyar) {
}