package com.example.ecommerce_system.store;

import com.example.ecommerce_system.dao.interfaces.ReviewDao;
import com.example.ecommerce_system.dao.interfaces.ProductDao;
import com.example.ecommerce_system.exception.review.ReviewRetrievalException;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.exception.DatabaseConnectionException;
import com.example.ecommerce_system.exception.review.ReviewCreationException;
import com.example.ecommerce_system.model.Review;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Repository
public class ReviewStore {
    private final DataSource dataSource;
    private final ReviewDao reviewDao;

    /**
     * Persist a new {@link Product} inside a transaction.</p>
     * Delegates to {@link ProductDao#save(java.sql.Connection, com.example.ecommerce_system.model.Product)}.
     * On success this method evicts relevant entries in the "products" cache via Spring Cache.
     */
    @CacheEvict(value = "reviews", allEntries = true)
    public Review createReview(Review review) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                this.reviewDao.save(conn, review);
                conn.commit();
                return review;
            } catch (DaoException e) {
                conn.rollback();
                throw new ReviewCreationException(review.getProduct().getProductId().toString());
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Load a page of reviews for the given product id.</p>
     * Results are loaded via {@link ReviewDao#findByProduct(java.sql.Connection, java.util.UUID, int, int)}
     */
    @Cacheable(value = "reviews", key = "'product:' + #productId + ':' + #limit + ':' + #offset")
    public List<Review> getReviewsByProduct(UUID productId, int limit, int offset) {
        try (Connection conn = dataSource.getConnection()) {
            return this.reviewDao.findByProduct(conn, productId, limit, offset);
        } catch (DaoException e) {
            throw new ReviewRetrievalException(productId.toString());
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }
}
