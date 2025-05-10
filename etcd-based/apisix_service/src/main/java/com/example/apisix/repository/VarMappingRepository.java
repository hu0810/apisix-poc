package com.example.apisix.repository;

import com.example.apisix.entity.PersonaVarMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface VarMappingRepository extends JpaRepository<PersonaVarMapping, Long> {
    List<PersonaVarMapping> findByPersonaTypeAndRouteTemplateCode(String personaType, String code);
}
