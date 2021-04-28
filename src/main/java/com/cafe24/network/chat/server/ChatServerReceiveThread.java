package com.cafe24.network.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;

public class ChatServerReceiveThread extends Thread {
	private String nickname;
	private Socket socket;
	private HashMap<String, Writer> writersHashMap;

	public ChatServerReceiveThread(Socket socket, HashMap<String, Writer> writersHashMap) {
		this.socket = socket;
		this.writersHashMap = writersHashMap;
	}

	@Override
	public void run() {
		// 1. Remote Host Information

		try {	// 데이터 통신
			// 2. 스트림 얻기
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

			// 3. 요청 처리
			while(true) {
				String request = br.readLine();
				if(request == null) {
					ChatServer.log("클라이언트(" + nickname + ")로부터 연결 끊김");
					doQuit(pw);
					break;
				}

				// 4. 프로토콜 분석
				String[] tokens = request.split(":");
				if("join".equals(tokens[0])) {
					doJoin(decodeBase64(tokens[1]), pw);
				} else if("message".equals(tokens[0])) {
					if(tokens.length < 2) {	// 아무것도 입력하지 않은 채로 send를 했을 경우, 빈 줄이 전달되도록 한다.
						doMessage("");
					} else {
						doMessage(tokens[1]);
					}
//				} else if("quit".equals(tokens[0])) {
//					doQuit(pw);
				} else if("whisper".equals(tokens[0])) { 
					if(tokens.length < 4) {	// 제대로 채워지지 않았을 경우, 귓속말 입력에 실패한다.
						ChatServer.log("잘못된 명령어 입력 (" + tokens[0] + ")");
					} else {
						// cmd에서 깨지는 문제 해결 위해 닉네임도 Base64로 encoding/decoding한다. 
						doWhisper(decodeBase64(tokens[1]), decodeBase64(tokens[2]), tokens[3]);
					}
				} else {
					ChatServer.log("에러: 알 수 없는 요청 (" + tokens[0] + ")");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 입장
	private void doJoin(String nickname, PrintWriter printWriter) {
		// 닉네임이 중복될 때, 다시 Join 시도를 하도록(=닉네임을 짓도록) 해야 한다.
		if(writersHashMap.containsKey(nickname)) {
			printWriter.println("join:fail");
			return;
		} else {
			this.nickname = nickname;
		}
		
		String data = nickname + "님이 참여하였습니다.";
		broadcast(data);

		/* Writer pool에 저장 */
		addWriter(printWriter);

		// 성공 시 ack. 확실히 동작하지만, 채팅 할 땐 거슬리므로 주석처리 해뒀다.
		printWriter.println("join:ok");
//		printWriter.flush();
	}
	private void addWriter(Writer writer) {
		synchronized(writersHashMap) {
			//listWriters.add(writer);
			writersHashMap.put(nickname, writer);
		}
	}

	// 퇴장
	private void doQuit(Writer writer) {
		removeWriter(writer);
		
		String data = nickname + "님이 퇴장하였습니다.";
		broadcast(data);
		
	}
	private void removeWriter(Writer writer) {
		synchronized(writersHashMap) {
			//listWriters.remove(writer);
			writersHashMap.remove(writer);
		}
	}

	// 메시지 입력
	private void doMessage(String data) {	
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		//!도움말인 경우 정보제공
		if(data.equals("!도움말")){
			String message = "[" + LocalDateTime.now().format(formatter) + "]" 
			  + "Server" + ": " + " 귓속말기능 :  /w <nickname> <message> ";
			broadcast(message);
							
			message = "[" + LocalDateTime.now().format(formatter) + "]" 
			  + "Server" + ": " + " 지우개기능 :  \"!지우개\" : 20칸 지움 ";
			broadcast(message);
		}else if(data.equals("!지우개")){
			//!지우개인 경우 채팅20칸 올림
			String message = " ";
			for(int i=1; i<20; i++) {
				broadcast(message);
			}
		}else{				
		// base64 decoding
		String message = "[" + LocalDateTime.now().format(formatter) + "]" + nickname + ": " + decodeBase64(data);
		broadcast(message);
		}
	}
	
	// 귓속말
	private void doWhisper(String senderName, String receiverName, String data) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		// base64 decoding
		String sendMessage = "[" + LocalDateTime.now().format(formatter) + "]" + senderName + "님으로부터의 귓속말: " + decodeBase64(data);	// 수신자에게 갈 메시지
		String myselfMessage = "[" + LocalDateTime.now().format(formatter) + "]" + receiverName + "님에게 귓속말: " + decodeBase64(data);	// 발신자에게 갈 메시지
		synchronized(writersHashMap) {
			// 해당되는 닉네임을 가진 사람에게만 broadcast한다.
			sendMessageToClient(writersHashMap.get(receiverName), sendMessage);
			// 보낸 사람에게는 자신이 보낸 메시지가 보여야 한다.
			sendMessageToClient(writersHashMap.get(senderName), myselfMessage);
		}
	}

	// 브로드캐스트
	private void broadcast(String data) {
		synchronized(writersHashMap) {
			for(Writer writer: writersHashMap.values()) {
				sendMessageToClient(writer, data);
			}
		}
	}
	
	// decode할 것이 많아져 별도의 메서드로 분리
	private String decodeBase64(String data) {
		return new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8); 
	}
	
	// 지정한 PrintWriter에게(=Client) message를 출력시킨다. 코드 중복 기능을 피하기 위해 별도의 메서드로 분리하였다.
	private void sendMessageToClient(Writer writer, String message) {
		PrintWriter printWriter = (PrintWriter)writer;
		printWriter.println(message);
		printWriter.flush();
	}
	
	
}
