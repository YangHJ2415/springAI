package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class ApiV1AiChatController {
    private final ChatModel chatModel;

    @GetMapping("/write")
    public String write(String msg) {
        String response = chatModel.call(msg);

        return response;
    }

    @GetMapping("/write2")
    public String write2(String msg) {
        // 1) 시스템 메시지: 쇼핑몰 규칙 안내
        String systemPrompt = """
                    너는 우리 쇼핑몰의 AI 챗봇이야.
                    - 너의 이름은 쇼피야.
                    - 우리 쇼핑몰 이름은 쇼핑천국이야.
                    - 고객에게는 정중히 인사해야 해.
                    - 제품 추천 전에는 재고 확인을 반드시 해.
                    - 반품/교환 기준은 '구매일로부터 7일 이내, 제품 미착용·미훼손'인 경우만 가능해.
                    - 개인정보는 절대 외부에 노출하지 마.
                """;

        // 2) 시스템 메시지 + 사용자 메시지 순서대로 설정
        String aiResponse = chatModel
                .call(
                        new SystemMessage(systemPrompt),
                        new UserMessage(msg)
                );

        return aiResponse;
    }

    // 채팅방을 생성하는 API
    @GetMapping("/room")
    public ResponseEntity<String> makeRoom() { // ResponseEntity는 HTTP 응답을 나타내는 객체입니다.
        String chatRoomCode= UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders(); // HttpHeaders는 HTTP 응답 헤더를 설정하는 데 사용됩니다.
        headers.setLocation(URI.create("./room/" + chatRoomCode)); // Location 헤더를 설정하여 리다이렉션 URL을 지정합니다.

        return ResponseEntity
                .status(HttpStatus.FOUND) // 302 Found 상태 코드를 사용하여 리다이렉션을 나타냅니다.
                .headers(headers) // 헤더에 Location을 설정하여 리다이렉션 URL을 지정합니다.
                .build(); // ResponseEntity를 빌드하여 반환합니다.
    }

    // 채팅방에 접속하는 API
    @GetMapping("/room/{chatRoomCode}")
    public String room(
            @PathVariable String chatRoomCode, // pathVariable은 URL 경로에서 변수를 추출하는 데 사용됩니다.
            @RequestParam(defaultValue = "") String msg, // requestParam은 쿼리 파라미터를 추출하는 데 사용됩니다.
            @RequestParam(defaultValue = "") String oldMsg // 이전 메시지를 추출하는 데 사용됩니다.
    ) {
        // 구분 용 접두어
        final String USER_PREFIX = "사용자 : ";
        final String AI_PREFIX = "LLM 답변 : ";

       // 채팅방 코드가 비어있으면 새로운 채팅방을 생성합니다.
       if (msg.isBlank()) return """
                안녕하세요!
                <br>
                여기 채팅 방 번호는 : %s 입니다.
                <br><br>
                <form>
                    <input required type="text" name="msg" placeholder="메시지를 입력하세요." autofocus>
                    <br><br>
                    <button type="submit">전송</button>
                </form>
                """.formatted(chatRoomCode);

        oldMsg = oldMsg.trim();

        String systemPrompt = """
                대답을 길게하지마
                """;

        // 메시지 리스트 생성 및, 시스템 메시지를 가장 앞에 추가
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // 이전 메세지, 메시지 배열 두 접두어를 인자로 보냄
        parseOldMessages(oldMsg, messages, USER_PREFIX, AI_PREFIX);

        // 입력받은 현재 메시지를 리스트에 추가
        messages.add(new UserMessage(msg));

        // LLM에 메시지 리스트를 전달하고 응답을 받음
        String response = chatModel.call(messages.toArray(Message[]::new));

        // oldMsg에 새 대화 내용을 이어붙임. (기존 대화oldMsg + 현재 입력msg + 응답response)
        StringBuilder updatedOldMsg = new StringBuilder(oldMsg);
        updatedOldMsg
                .append("\n\n").append(USER_PREFIX).append(msg)
                .append("\n\n").append(AI_PREFIX).append(response);

        // 채팅방에 접속한 사용자에게 메시지를 보여주는 HTML 폼을 반환합니다.
        return """
                <form>
                    <textarea name="oldMsg" rows="30" cols="30">%s</textarea>
                    <br>
                    <input required type="text" name="msg" placeholder="메시지를 입력하세요." autofocus>
                    <br>
                    <button type="submit">전송</button>
                </form>
                """.formatted(updatedOldMsg.toString().trim());
    }

    // 리스트에 사용자 메시지와 AI 답변을 분리해 추가
    private void parseOldMessages(String oldMsg, List<Message> messages, String userPrefix, String aiPrefix) {

        // 두 줄 띄우기를 기준으로 사용자와 AI 메시지를 나눔
        for (String part : oldMsg.split("\\n\\n")) {
            part = part.trim();

            // "사용자 : " 로 시작하는 경우 접두어 제거 후 추가
            if (part.startsWith(userPrefix)) {
                messages.add(new UserMessage(part.replaceFirst("^" + userPrefix, "")));
                // "LLM 담변 : " 로 시작하는 경우 접두어 제거후 추가
            } else if (part.startsWith(aiPrefix)) {
                messages.add(new AssistantMessage(part.replaceFirst("^" + aiPrefix, "")));
            }
        }
    }
}