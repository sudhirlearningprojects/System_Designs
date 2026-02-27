package org.sudhir512kj.alertmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.alertmanager.model.AlertDelivery;
import org.sudhir512kj.alertmanager.model.DeliveryStatus;

import java.util.List;

@Repository
public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, String> {
    List<AlertDelivery> findByStatusIn(List<DeliveryStatus> statuses);
}
