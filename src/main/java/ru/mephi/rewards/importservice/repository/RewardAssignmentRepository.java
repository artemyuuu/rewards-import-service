package ru.mephi.rewards.importservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mephi.rewards.importservice.entity.RewardAssignment;

import java.time.OffsetDateTime;

@Repository
public interface RewardAssignmentRepository extends JpaRepository<RewardAssignment, Long> {

    public boolean existsByEmployeeIdAndRewardIdAndAwardedAt(Long employeeId, Long rewardId, OffsetDateTime awardedAt);

}
