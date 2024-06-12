package jp.co.metateam.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.metateam.library.model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    List<Stock> findAll();

    List<Stock> findByDeletedAtIsNull();

    List<Stock> findByDeletedAtIsNullAndStatus(Integer status);

	Optional<Stock> findById(String id);

    List<Stock> findByBookMstIdAndStatus(Long book_id,Integer status);
    
    @Query(value = "SELECT bm.title, COUNT(st.id) AS availableStockCount "+
                    "FROM book_mst bm "+
                    "LEFT JOIN stocks st ON bm.id = st.book_id AND st.status = 0 "+
                    "GROUP BY bm.title", nativeQuery = true )
            
        List<Object[]> findAllTitleAndAvailableStockCount();
        
   
}
