package org.wonik.authserver.domain.auth.loginhistory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

	List<LoginHistory> findAllByLoginIdOrderByIdAsc(String loginId);
}
