package com.example.apisix.repository;

import com.example.apisix.entity.PersonaVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaVariableRepository extends JpaRepository<PersonaVariable, Long> {
    List<PersonaVariable> findByPersonaType(String personaType);
    Optional<PersonaVariable> findByPersonaTypeAndVarKey(String personaType, String varKey);
}
