package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Alternativa;

@Repository
public interface AlternativaRepository extends JpaRepository<Alternativa, Integer> {
	
}