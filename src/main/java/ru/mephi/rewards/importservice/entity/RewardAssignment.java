// RewardAssignment.java
package ru.mephi.rewards.importservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;                  // ← добавить для exclude
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "reward_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "reward_id", "awarded_at"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude                    // ← чтобы не было рекурсии в логах
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "reward_id", nullable = false)
    private Long rewardId;

    @Column(name = "reward_name", nullable = false)
    private String rewardName;

    @Column(name = "awarded_at", nullable = false)
    private OffsetDateTime awardedAt;
}
