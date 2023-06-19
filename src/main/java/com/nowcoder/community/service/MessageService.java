package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author : Real
 * @date : 2021/12/6 13:44
 * @description : 私信 Service 层
 */
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationsCount(int userId) {
        return messageMapper.selectConversationsCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLettersCount(String conversationId) {
        return messageMapper.selectedLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message) {
        // 需要对私信的内容进行过滤，避免影响用户阅读
        //htmlescape方法将 HTML 特殊字符转义为 HTML 通用转义序列
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    /**
     * 读取消息，将消息状态变为已读
     *
     * @param ids 会话的 ID 集合
     * @return 更新状态的行数
     */
    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }

    /**
     * 查询最新的通知
     *
     * @param userId 用户 ID
     * @param topic  主题
     * @return Message 对象
     */
    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    /**
     * 查询通知数量
     *
     * @param userId 用户 ID
     * @param topic  主题
     * @return 通知总数
     */
    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }

    /**
     * 查询未读通知数量
     *
     * @param userId 用户 ID
     * @param topic  主题
     * @return 未读通知数量
     */
    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    /**
     * 查询通知列表
     *
     * @param userId 用户 ID
     * @param topic  通知类型
     * @param offset 偏移量
     * @param limit  分页限制
     * @return 通知列表
     */
    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }

}
