package com.back;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class ApiV1AiInMemoryChatController {

    // ChatClient는 AI와의 대화를 관리하는 클라이언트입니다.
    private final ChatClient chatClient;
    // ChatMemory는 대화의 상태를 저장하고 관리하는 객체입니다.
    private final ChatMemory chatMemory;

    // 채팅방을 생성하는 API
    @GetMapping("/room")
    public ResponseEntity<Void> makeRoom() { // ResponseEntity는 HTTP 응답을 나타내는 객체입니다.
        String chatRoomCode = UUID.randomUUID().toString();

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
            @RequestParam(defaultValue = "") String msg // requestParam은 쿼리 파라미터를 추출하는 데 사용됩니다.
    ) {

        if (msg.isBlank()) {
            return """
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
        }

        // 채팅방 코드에 해당하는 메모리를 가져옵니다.
        List<Message> memories = chatMemory.get(chatRoomCode);

        // 만약 메모리가 비어있다면, 새로운 메모리를 생성합니다.
        String response = chatClient // ChatClient를 사용하여 AI와 대화를 시작합니다.
                .prompt() // 프롬프트를 시작합니다.
                .messages(memories) // 이전 메시지들을 추가합니다.
                .user(msg)// 현재 사용자의 메시지를 추가합니다.
                .call() // 메시지를 AI에게 전달하고 응답을 받습니다.
                .content(); // 응답의 내용을 가져옵니다.

        // 응답이 null이거나 비어 있으면 에러 메시지를 반환
        if (response == null || response.isEmpty()) {
            return "죄송합니다. 응답을 생성할 수 없습니다.";
        }

        return """
                <form>
                    <textarea name="oldMsg" rows="30" cols="30">%s</textarea>
                    <br>
                    <input required type="text" name="msg" placeholder="메시지를 입력하세요." autofocus>
                    <br>
                    <button type="submit">전송</button>
                </form>
                """.formatted(response);
    }
}