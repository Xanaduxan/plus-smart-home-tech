package ru.yandex.practicum.analyzer.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.yandex.practicum.analyzer.entity.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Scenario> findByHubId(String hubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Scenario> findByHubIdAndName(String hubId, String name);
}