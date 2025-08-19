package Capstone.Capstone.repository;

import Capstone.Capstone.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like,Long> {
    List<Like> findByCommunityIdAndUserId(Long postId, String userId);
}
