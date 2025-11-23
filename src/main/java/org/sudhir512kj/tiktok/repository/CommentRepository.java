package org.sudhir512kj.tiktok.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.tiktok.model.Comment;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Long videoId, Pageable pageable);
}
