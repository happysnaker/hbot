package io.github.happysnaker.hbotcore.gpt;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author happysnaker
 * @Date 2023/4/18
 * @Email happysnaker@foxmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthConfig {
    @JSONField(name = "access_token")
    private String accessToken;

    @JSONField(name = "conversationId")
    private String conversationId;

    @JSONField(name = "parentId")
    private String parentId;
}
