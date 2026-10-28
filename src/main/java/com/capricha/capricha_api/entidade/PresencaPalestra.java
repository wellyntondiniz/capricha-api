package com.capricha.capricha_api.entidade;

import java.time.Instant;
import com.capricha.capricha_api.model.Palestra;
import jakarta.persistence.*;

/** Presença em palestra não é inscrição/pontuação em evento. */
@Entity
@Table(name = "presenca_palestra", uniqueConstraints = @UniqueConstraint(
        name = "uk_presenca_usuario_palestra", columnNames = {"usuario_id", "palestra_id"}))
public class PresencaPalestra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "palestra_id", nullable = false)
    private Palestra palestra;
    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    protected PresencaPalestra() {}
    public PresencaPalestra(Usuario usuario, Palestra palestra, Instant instante) {
        this.usuario = usuario;
        this.palestra = palestra;
        this.checkedInAt = instante;
    }
    public Long getId() { return id; }
    public Palestra getPalestra() { return palestra; }
    public Instant getCheckedInAt() { return checkedInAt; }
}
