package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author : Real
 * @date : 2021/12/2 21:49
 * @description : 评论 DAO 层
 */
@Mapper
public interface CommentMapper {

    /**
     * 分页查询评论
     *
     * @param entityId
     * @param entityType
     * @param offset
     * @param limit
     * @return
     */
    List<Comment> selectCommentsByEntity(int entityId, int entityType, int offset, int limit);

    /**
     * 查询帖子评论的数量
     *
     * @param entityId
     * @param entityType
     * @return
     */
    int selectCountByEntity(int entityId, int entityType);

    /**
     * 添加评论
     *
     * @param comment 评论对象
     * @return 插入的行数
     */
    int insertComment(Comment comment);

    /**
     * 根据评论 ID 查询评论
     *
     * @param id 评论 ID
     * @return comment 对象
     */
    Comment selectCommentById(int id);

    /**
     * 根据用户 ID 查询发布的评论
     *
     * @param userId 用户 ID
     * @return 评论/回复数量
     */
    int selectCountByUserId(int userId);

    /**
     * 根据用户 ID 查询发布的评论
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit  分页限制
     * @return Comment 列表
     */
    List<Comment> selectCommentsByUserId(int userId, int offset, int limit);

}
