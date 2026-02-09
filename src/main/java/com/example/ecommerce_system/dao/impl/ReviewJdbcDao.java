package com.example.ecommerce_system.dao.impl;

import com.example.ecommerce_system.dao.interfaces.ReviewDao;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.Review;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Repository
public class ReviewJdbcDao implements ReviewDao {

    private static final String FIND_BY_PRODUCT = """
        SELECT review_id, product_id, customer_id, rating, comment::text, created_at
        FROM review
        WHERE product_id = ?
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        """;

    @Override
    public List<Review> findByProduct(Connection conn, UUID productId, int limit, int offset)
            throws DaoException {

        List<Review> reviews = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PRODUCT)) {

            ps.setObject(1, productId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    reviews.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch reviews for product " + productId, e);
        }
        return reviews;
    }

    private Review map(ResultSet rs) throws SQLException {
        return new Review(
                rs.getObject("review_id", UUID.class),
                null,
                rs.getObject("customer_id", UUID.class),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    @Override
    public void save(Connection conn, Review review) throws DaoException {
        String sql = """
            INSERT INTO review (review_id, product_id, customer_id, rating, comment, created_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, review.getReviewId());
            ps.setObject(2, review.getProduct().getProductId());
            ps.setObject(3, review.getCustomerId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getComment());
            ps.setTimestamp(6, Timestamp.from(review.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to save review", e);
        }
    }
}
