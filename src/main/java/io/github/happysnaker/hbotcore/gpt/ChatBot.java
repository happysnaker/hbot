package io.github.happysnaker.hbotcore.gpt;


import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import io.github.happysnaker.hbotcore.logger.Logger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class ChatBot {
    /**
     * 配置文件(包括 access_token)
     * {
     * "email": "OpenAI account email",
     * "password": "OpenAI account password",
     * "session_token": "<session_token>"
     * "access_token": "<access_token>"
     * "proxy": "<proxy_url_string>",
     * "paid": True/False, # whether this is a plus account
     * }
     */
    private String config;

    /**
     * 会话id
     */
    private String conversationId;

    /**
     * 上下文id
     */
    private String parentId;

    private HashMap<String, Object> conversationMapping = new HashMap<>();

    private Queue<String> conversationIdPrevQueue = new LinkedList<>();

    private Queue<String> parentIdPrevQueue = new LinkedList<>();

    private Boolean lazyLoading = true;

    private HttpRequest.Builder requestBuilder;


    public ChatBot() {
        this(ConfigureUtil.getConfigure());
    }

    public ChatBot(@NonNull AuthConfig config) {
        if (!StrUtil.isEmpty(config.getAccessToken())) {
            String accessToken = config.getAccessToken();
            if (StrUtil.isBlank(accessToken)) {
                throw new RuntimeException("No access_token found.");
            }
            this.conversationId = config.getConversationId();
            this.parentId = config.getParentId();
            this.requestBuilder = checkCredentials(accessToken);
        }

        //TODO 邮箱登陆，目前只支持access_token
    }

    private HttpRequest.Builder checkCredentials(String accessToken) {
        return HttpRequest.newBuilder()
                .header(Header.ACCEPT.getValue(), "text/event-stream")
                .header(Header.AUTHORIZATION.getValue(), "Bearer " + accessToken)
                .header(Header.CONTENT_TYPE.getValue(), "application/json")
                .header("X-Openai-Assistant-App-Id", "")
                .header(Header.ACCEPT_LANGUAGE.getValue(), "en-US,en;q=0.9")
                .header(Header.REFERER.getValue(), "https://chat.openai.com/chat");
    }

    /**
     * 向gpt提问
     *
     * @param prompt         问题内容
     * @param conversationId UUID 会话id 用于继续上一次对话 默认为 null
     * @param parentId       UUID 上下文id 用于保持上下文 默认为 null
     * @param model          模型名称 比如(gpt-4) 默认为 null
     * @param timeout        超时时间 默认为 360s
     * @return
     */
    public String ask(String prompt, String conversationId, String parentId, String model, Integer timeout) {

        // 设置 parentId 的前提是 conversationId 已经存在
        if (!StrUtil.isBlank(parentId) && StrUtil.isBlank(conversationId)) {
            Logger.error("conversation_id must be set once parent_id is set");
            throw new IllegalArgumentException("conversation_id must be set once parent_id is set");
        }

        if (!StrUtil.isBlank(conversationId) && !conversationId.equals(this.conversationId)) {
            Logger.debug("Updating to new conversation by setting parent_id to None");
            this.parentId = null;
        }

        conversationId = StrUtil.isBlank(conversationId) ? this.conversationId : conversationId;
        parentId = StrUtil.isBlank(parentId) ? this.parentId : parentId;

        if (StrUtil.isBlank(conversationId) && StrUtil.isBlank(parentId)) {
            parentId = UUID.randomUUID().toString();
            Logger.debug("New conversation, setting parent_id to new UUID4: {}", parentId);
        }

        if (!StrUtil.isBlank(conversationId) && StrUtil.isBlank(parentId)) {
            if (!this.conversationMapping.containsKey(conversationId)) {
                if (this.lazyLoading) {
                    Logger.debug("Conversation ID {} not found in conversation mapping, try to get conversation history for the given ID", conversationId);
                    try {
                        HashMap<String, Object> history = getMsgHistory(conversationId);
                        this.conversationMapping.put(conversationId, history.get("current_node"));
                    } catch (Exception e) {
                        // 忽略异常
                    }
                } else {
                    Logger.debug("Conversation ID {} not found in conversation mapping, mapping conversations", conversationId);
                    mapConversations();
                }
            }
            if (this.conversationMapping.containsKey(conversationId)) {
                Logger.debug("Conversation ID {} found in conversation mapping, setting parent_id to {}", conversationId, MapUtil.getStr(this.conversationMapping, conversationId));
                parentId = MapUtil.getStr(this.conversationMapping, conversationId);
            } else {
                conversationId = null;
                parentId = UUID.randomUUID().toString();
            }
        }

        //构造消息体
        final String finalConversationId = conversationId;
        final String finalParentId = parentId;
        final String finalModel = model;
        HashMap<String, Object> data = new HashMap<>() {{
            put("action", "next");
            put("conversation_id", finalConversationId);
            put("parent_message_id", finalParentId);
            put("model", getModel(finalModel));
            put("messages", new ArrayList<>() {{
                add(new HashMap<String, Object>() {{
                    put("id", UUID.randomUUID().toString());
                    put("role", "user");
                    put("author", new HashMap<String, String>() {{
                        put("role", "user");
                    }});
                    put("content", new HashMap<String, Object>() {{
                        put("content_type", "text");
                        put("parts", new ArrayList<>() {{
                            add(prompt);
                        }});
                    }});
                }});
            }});
        }};

        Logger.debug("Sending the payload");
        Logger.debug(JSONUtil.toJsonPrettyStr(data));

        this.conversationIdPrevQueue.offer(MapUtil.getStr(data, "conversation_id"));
        this.parentIdPrevQueue.offer(MapUtil.getStr(data, "parent_message_id"));

        HttpRequest request = this.requestBuilder
                .uri(URI.create(EnvironmentConstant.BASE_URL))
                .timeout(timeout == null ? EnvironmentConstant.TIMEOUT : Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(JSONUtil.toJsonStr(data)))
                .build();
        HttpClient client = HttpClient.newHttpClient();

        try {
            //流式处理响应
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            checkResponse(response);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            String line;
            boolean done = false;
            String msg = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.toLowerCase().equals("internal server error")) {
                    Logger.error("Internal server error: {}", line);
                    throw new RuntimeException("Internal server error");
                }
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                if (line.startsWith("data: ")) {
                    line = line.substring(6);
                }
                if (line.equals("[DONE]")) {
                    done = true;
                    break;
                }
                line = line.replace("\\\"", "\"");
                line = line.replace("\\'", "'");
                line = line.replace("\\\\", "\\");
                HashMap lineMap;
                try {
                    lineMap = JSONUtil.toBean(line, HashMap.class);
                } catch (Exception e) {
                    continue;
                }

                if (!checkFields(lineMap) || response.statusCode() != 200) {
                    Logger.error("Field missing");
                    Logger.error(response.body().toString());
                    //TODO 根据状态码返回详细错误信息
                    throw new RuntimeException("error, code is " + response.statusCode());
                }
                msg = MapUtil.get(MapUtil.get(MapUtil.get(lineMap, "message", Map.class),
                                "content", Map.class),
                        "parts", List.class).get(0).toString();
                if (prompt.equals(msg)) {
                    continue;
                }
                conversationId = MapUtil.getStr(lineMap, "conversation_id");
                parentId = MapUtil.getStr(MapUtil.get(lineMap, "message", Map.class), "id");
                try {
                    model = MapUtil.getStr(MapUtil.get(MapUtil.get(lineMap, "message", Map.class),
                                    "metadata", Map.class),
                            "model_slug");
                } catch (Exception e) {
                    model = null;
                }

                Logger.debug("Received message: {}", msg);
                Logger.debug("Received conversation_id: {}", conversationId);
                Logger.debug("Received parent_id: {}", parentId);
            }

            if (!done) {
                //TODO
                throw new RuntimeException("Chatgpt 异常中止了本次对话，请重试");
            }
            this.conversationMapping.put(conversationId, parentId);
            if (!StrUtil.isBlank(parentId)) {
                this.parentId = parentId;
            }
            if (!StrUtil.isBlank(conversationId)) {
                this.conversationId = conversationId;
            }

            return msg;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkFields(HashMap lineMap) {
        try {
            MapUtil.get(MapUtil.get(lineMap, "message", Map.class),
                    "content", Map.class);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void checkResponse(HttpResponse response) {
        if (response.statusCode() != 200) {
            Logger.error("Response status code: {}", response.statusCode());
            Logger.error("Response body: {}", response.body());
            throw new RuntimeException("Response status code: " + response.statusCode() + ", message: " + response.body());
        }
    }

    //TODO
    private void mapConversations() {

    }

    /**
     * 获取历史消息
     *
     * @param conversationId
     * @return
     */
    private HashMap<String, Object> getMsgHistory(String conversationId) {
        HttpRequest request = this.requestBuilder
                .uri(URI.create(EnvironmentConstant.BASE_URL + "/" + conversationId))
                .GET()
                .build();
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
            HashMap map = JSONUtil.toBean(response.body(), HashMap.class);
            return JSONUtil.toBean(response.body(), HashMap.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getModel(String model) {
        JSONObject jsonConfig = JSONUtil.parseObj(config);
        return model = model != null ? model
                : jsonConfig.getStr("model") != null ? jsonConfig.getStr("model")
                : Boolean.TRUE.equals(jsonConfig.get("paid")) ? "text-davinci-002-render-paid"
                : "text-davinci-002-render-sha";
    }

    public String ask(String prompt) {
        return this.ask(prompt, 36000);
    }

    public String ask(String prompt, Integer timeout) {
        return this.ask(prompt, null, null, null, timeout);
    }
}
