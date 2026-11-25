package org.wonik.coreviaai_be.domain.menu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.menu.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	List<Menu> findAllByOrderBySortOrderAscIdAsc();
}
