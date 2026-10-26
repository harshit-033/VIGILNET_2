package in.gov.aicte.platform.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertDeliveryAttemptRepository extends JpaRepository<AlertDeliveryAttempt, Long> {

    List<AlertDeliveryAttempt> findByAlertIdOrderByAttemptedAtAsc(Long alertId);
}
