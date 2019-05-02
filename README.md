# Chatting (Server/Client)
## 실행법
### 서버
1. chatcs 디렉터리로 이동
2. `java -cp bin\chatcs.jar com.cafe24.network.chat.server.ChatServer` 입력

### 클라이언트
1. chatcs 디렉터리로 이동
2. `java -cp bin\chatcs.jar com.cafe24.network.chat.client.ChatClientApp` 입력

## 구현 기능
- 단체 채팅방(방은 1개)
- 닉네임 등록해야 한다. (중복 닉네임은 허용한다.)
- 입장했을 때 입장한 본인을 제외하고 "<nickname>님이 참여하였습니다." 메시지가 출력된다.
- 퇴장했을 때 퇴장한 본인을 제외하고 "<nickname>님이 퇴장하였습니다." 메시지가 출력된다.
- 다른 사용자의 메시지는 키보드로 입력 도중에 전달되어 화면에 출력된다.
   - 출력 양식: [yyyy-MM-dd HH:mm:ss] <nickname>: <message>
- 방을 나올 때는 프로그램을 직접 종료해야 한다. quit 명령어는 구현하지 않았다.
- 귓속말 기능(중복 닉네임을 허용했기 때문에 같은 닉네임을 가진 사람이 2명이면 2명 모두에게 귓속말이 간다. 역으로, 발신자 메시지의 경우 발신자의 닉네임이 중복되어 있다면 중복된 닉네임의 또 다른 유저들이 귓속말 발신 사실을 확인할 수 있다. 또한, 채팅방에 참여하고 있지 않은 유저의 닉네임으로 보낼 경우 에러 메시지가 출력되지 않는다.)
   - 명령어에 인자가 충분하지 않은 경우 일반 메시지로써 나간다.

## 프로토콜 목록
- join:<nickname>
   - 채팅방에 들어온다.
- message:<message>
   - 메시지를 보낸다.
- quit:<nickname>
   - 채팅 프로그램을 종료한다.(채팅방에서 나간다.)
- whisper:<senderNickname>:<receiverName>:<message>
   - 지정한 닉네임을 가진 사람에게 메시지를 보낸다.
   - Program에서 보내는 법: /w <nickname> <message>

## 비고
- 인코딩 방식에서 "UTF-8"을 직접 입력할 경우 예외 처리를 해줘야 해서 직접 입력하는 대신 `StandardCharsets.UTF_8`을 사용하였다.
- 클라이언트 종료 시 닫힌 소켓에 대해 입력 대기를 하는 상황에 대해 예외를 처리하였다. (`ChatClientReceiveThread`)
- 원본 메시지는 Base64 인코딩을 통해 다른 특수문자를 입력해도 원활하게 처리할 수 있도록 하였다.
