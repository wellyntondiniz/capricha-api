package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.Palestra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PalestraRepository extends JpaRepository<Palestra, Long> {

    List<Palestra> findByEvento(String evento);

}