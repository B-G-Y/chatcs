package com.cafe24.network.chat.client;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClientApp {
	private static final String SERVER_IP = "127.0.0.1";
	public static void main(String[] args) {
		String name = null;
		Scanner scanner = new Scanner(System.in);

		while( true ) {
			System.out.println("대화명을 입력하세요.");
			System.out.print(">>> ");
			name = scanner.nextLine();
			 
			if (name.isEmpty()) {
				System.out.println("대화명은 한글자 이상 입력해야 합니다.\n");
				continue;
			}
			break;
		}
		
		// 1. 소켓 만들고
		// 2. IOStream
		// 3. join 프로토콜<닉네임 등록> 성공
		Socket socket = null;
		
		try {
			socket = new Socket();
			// 서버 연결
			socket.connect(new InetSocketAddress(SERVER_IP, com.cafe24.network.chat.server.ChatServer.PORT));
			System.out.println("[client] connected");
			
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true); // auto flush => true
			
			// 이름 자체도 encoding해서 :가 들어가도 제대로 생성되도록 한다.
			pr.println("join:" + ChatWindow.encodeBase64(name));
			System.out.println(name + "님, 채팅방에 입장합니다.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		scanner.close();

		new ChatWindow(socket, name).show();
	}
}
