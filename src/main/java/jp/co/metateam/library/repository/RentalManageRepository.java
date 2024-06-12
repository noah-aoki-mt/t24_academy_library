package jp.co.metateam.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.Date;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.metateam.library.model.RentalManage;

@Repository
public interface RentalManageRepository extends JpaRepository<RentalManage, Long> {
    List<RentalManage> findAll();

    Optional<RentalManage> findById(Long id);

    List<RentalManage> findAllByStockIdAndStatusIn(String stockId, List<Integer> statusList);

    @Query(value = "SELECT COUNT(*) AS total FROM  rental_manage rm JOIN stocks s ON rm.stock_id = s.id " +
            "JOIN book_mst bm  ON s.book_id  = bm.id " +
            "WHERE rm.expected_rental_on <= :day AND :day <= rm.expected_return_on AND bm.title = :title", nativeQuery = true)
    Long findByUnAvailableDayCount(@Param("day") Date day, @Param("title") String title);

    @Query(value = "SELECT s.id AS count FROM stocks s " +
            "JOIN book_mst bm ON s.book_id = bm.id " +
            "LEFT JOIN rental_manage rm ON s.id = rm.stock_id " +
            "WHERE (rm.expected_rental_on) >= :day OR :day <= rm.expected_return_on OR rm.stock_id IS NULL AND bm.title = :title AND s.status = '0'" 
            , nativeQuery = true)

    List<String> findByAvailableStockId(@Param("day") Date day, @Param("title") String title);
}
