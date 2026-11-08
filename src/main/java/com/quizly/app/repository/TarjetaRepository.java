package com.quizly.app.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quizly.app.model.entity.Tarjeta;

public interface TarjetaRepository extends JpaRepository<Tarjeta, Long> {

    // Toda operación sobre una tarjeta exige que el mazo pertenezca al usuario
    Optional<Tarjeta> findByIdAndMazoIdAndMazoUsuarioId(Long id, Long mazoId, Long usuarioId);

    List<Tarjeta> findByMazoIdOrderByIdAsc(Long mazoId);

    long countByMazoId(Long mazoId);

    Optional<Tarjeta> findFirstByMazoIdAndProximaRevisionLessThanEqualOrderByProximaRevisionAscIdAsc(
            Long mazoId, LocalDateTime ahora);

    long countByMazoIdAndProximaRevisionLessThanEqual(Long mazoId, LocalDateTime ahora);

    Optional<Tarjeta> findFirstByMazoIdAndProximaRevisionAfterOrderByProximaRevisionAscIdAsc(
            Long mazoId, LocalDateTime ahora);

    List<Tarjeta> findByMazoUsuarioIdAndPreguntaIdIn(Long usuarioId, Collection<Long> preguntaIds);

    @Query("SELECT t.anverso FROM Tarjeta t WHERE t.mazo.id = :mazoId")
    List<String> findAnversosByMazoId(@Param("mazoId") Long mazoId);
}
