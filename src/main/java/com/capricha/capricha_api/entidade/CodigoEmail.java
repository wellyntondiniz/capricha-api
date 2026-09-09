package com.capricha.capricha_api.entidade;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "codigo_email", indexes = @Index(columnList = "email,criado"))
public class CodigoEmail {
    @Id public String id;
    @Column(nullable = false) public String email;
    @Column(nullable = false, length = 64) public String hash;
    @Column(nullable = false) public Instant criado;
    @Column(nullable = false) public Instant expira;
    public int tentativas;
    public boolean validado;
    public boolean utilizado;
}
