
package com.icesi.cuidapetproyect.repository;

import com.icesi.cuidapetproyect.model.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentReportRepository
        extends JpaRepository<IncidentReport, Long> {

    List<IncidentReport> findByReporter_Id(Long reporterId);

    List<IncidentReport> findByReportStatus(String reportStatus);
}
