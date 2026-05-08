package moderation;

import java.util.UUID;


/**
 * 一条活跃举报记录，表示某个用户在某个时间举报了某条消息。
 *
 * @param message 被举报消息的 UUID
 * @param user 发起举报的用户 UUID
 * @param timestamp 举报发生的时间戳
 */
public record Report(UUID message, UUID user, long timestamp) {}
