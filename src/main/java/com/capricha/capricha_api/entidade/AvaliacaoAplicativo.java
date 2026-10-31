package com.capricha.capricha_api.entidade;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "avaliacao_aplicativo")
public class AvaliacaoAplicativo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    // 1. TAM — Facilidade de uso percebida (PEOU)
    @Column(nullable = false)
    private Integer peou01;

    @Column(nullable = false)
    private Integer peou02;

    @Column(nullable = false)
    private Integer peou03;

    @Column(nullable = false)
    private Integer peou04;

    @Column(nullable = false)
    private Integer peou05;

    @Column(nullable = false)
    private Integer peou06;

    // 2. TAM — Utilidade percebida (PU)
    @Column(nullable = false)
    private Integer pu01;

    @Column(nullable = false)
    private Integer pu02;

    @Column(nullable = false)
    private Integer pu03;

    @Column(nullable = false)
    private Integer pu04;

    @Column(nullable = false)
    private Integer pu05;

    @Column(nullable = false)
    private Integer pu06;

    // 3. GAMEFULQUEST — Realização (AC)
    @Column(nullable = false)
    private Integer ac01;

    @Column(nullable = false)
    private Integer ac02;

    @Column(nullable = false)
    private Integer ac03;

    @Column(nullable = false)
    private Integer ac04;

    @Column(nullable = false)
    private Integer ac05;

    @Column(nullable = false)
    private Integer ac06;

    @Column(nullable = false)
    private Integer ac07;

    @Column(nullable = false)
    private Integer ac08;

    // 4. GAMEFULQUEST — Experiência social (SE)
    @Column(nullable = false)
    private Integer se01;

    @Column(nullable = false)
    private Integer se02;

    @Column(nullable = false)
    private Integer se03;

    @Column(nullable = false)
    private Integer se04;

    @Column(nullable = false)
    private Integer se05;

    @Column(nullable = false)
    private Integer se06;

    @Column(nullable = false)
    private Integer se07;

    @Column(nullable = false)
    private Integer se08;

    // 5. Modelo 3C — Comunicação (COM)
    @Column(nullable = false)
    private Integer com01;

    @Column(nullable = false)
    private Integer com02;

    @Column(nullable = false)
    private Integer com03;

    // 6. Modelo 3C — Coordenação (COO)
    @Column(nullable = false)
    private Integer coo01;

    @Column(nullable = false)
    private Integer coo02;

    @Column(nullable = false)
    private Integer coo03;

    // 7. Modelo 3C — Cooperação (COP)
    @Column(nullable = false)
    private Integer cop01;

    @Column(nullable = false)
    private Integer cop02;

    @Column(nullable = false)
    private Integer cop03;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant respondidaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Integer getPeou01() { return peou01; }
    public void setPeou01(Integer peou01) { this.peou01 = peou01; }
    public Integer getPeou02() { return peou02; }
    public void setPeou02(Integer peou02) { this.peou02 = peou02; }
    public Integer getPeou03() { return peou03; }
    public void setPeou03(Integer peou03) { this.peou03 = peou03; }
    public Integer getPeou04() { return peou04; }
    public void setPeou04(Integer peou04) { this.peou04 = peou04; }
    public Integer getPeou05() { return peou05; }
    public void setPeou05(Integer peou05) { this.peou05 = peou05; }
    public Integer getPeou06() { return peou06; }
    public void setPeou06(Integer peou06) { this.peou06 = peou06; }

    public Integer getPu01() { return pu01; }
    public void setPu01(Integer pu01) { this.pu01 = pu01; }
    public Integer getPu02() { return pu02; }
    public void setPu02(Integer pu02) { this.pu02 = pu02; }
    public Integer getPu03() { return pu03; }
    public void setPu03(Integer pu03) { this.pu03 = pu03; }
    public Integer getPu04() { return pu04; }
    public void setPu04(Integer pu04) { this.pu04 = pu04; }
    public Integer getPu05() { return pu05; }
    public void setPu05(Integer pu05) { this.pu05 = pu05; }
    public Integer getPu06() { return pu06; }
    public void setPu06(Integer pu06) { this.pu06 = pu06; }

    public Integer getAc01() { return ac01; }
    public void setAc01(Integer ac01) { this.ac01 = ac01; }
    public Integer getAc02() { return ac02; }
    public void setAc02(Integer ac02) { this.ac02 = ac02; }
    public Integer getAc03() { return ac03; }
    public void setAc03(Integer ac03) { this.ac03 = ac03; }
    public Integer getAc04() { return ac04; }
    public void setAc04(Integer ac04) { this.ac04 = ac04; }
    public Integer getAc05() { return ac05; }
    public void setAc05(Integer ac05) { this.ac05 = ac05; }
    public Integer getAc06() { return ac06; }
    public void setAc06(Integer ac06) { this.ac06 = ac06; }
    public Integer getAc07() { return ac07; }
    public void setAc07(Integer ac07) { this.ac07 = ac07; }
    public Integer getAc08() { return ac08; }
    public void setAc08(Integer ac08) { this.ac08 = ac08; }

    public Integer getSe01() { return se01; }
    public void setSe01(Integer se01) { this.se01 = se01; }
    public Integer getSe02() { return se02; }
    public void setSe02(Integer se02) { this.se02 = se02; }
    public Integer getSe03() { return se03; }
    public void setSe03(Integer se03) { this.se03 = se03; }
    public Integer getSe04() { return se04; }
    public void setSe04(Integer se04) { this.se04 = se04; }
    public Integer getSe05() { return se05; }
    public void setSe05(Integer se05) { this.se05 = se05; }
    public Integer getSe06() { return se06; }
    public void setSe06(Integer se06) { this.se06 = se06; }
    public Integer getSe07() { return se07; }
    public void setSe07(Integer se07) { this.se07 = se07; }
    public Integer getSe08() { return se08; }
    public void setSe08(Integer se08) { this.se08 = se08; }

    public Integer getCom01() { return com01; }
    public void setCom01(Integer com01) { this.com01 = com01; }
    public Integer getCom02() { return com02; }
    public void setCom02(Integer com02) { this.com02 = com02; }
    public Integer getCom03() { return com03; }
    public void setCom03(Integer com03) { this.com03 = com03; }

    public Integer getCoo01() { return coo01; }
    public void setCoo01(Integer coo01) { this.coo01 = coo01; }
    public Integer getCoo02() { return coo02; }
    public void setCoo02(Integer coo02) { this.coo02 = coo02; }
    public Integer getCoo03() { return coo03; }
    public void setCoo03(Integer coo03) { this.coo03 = coo03; }

    public Integer getCop01() { return cop01; }
    public void setCop01(Integer cop01) { this.cop01 = cop01; }
    public Integer getCop02() { return cop02; }
    public void setCop02(Integer cop02) { this.cop02 = cop02; }
    public Integer getCop03() { return cop03; }
    public void setCop03(Integer cop03) { this.cop03 = cop03; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public Instant getRespondidaEm() { return respondidaEm; }
    public void setRespondidaEm(Instant respondidaEm) { this.respondidaEm = respondidaEm; }
}
