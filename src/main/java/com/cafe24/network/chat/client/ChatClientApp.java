package com.cafe24.network.chat.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClientApp {
	private static final String SERVER_IP = "127.0.0.1";
	//	private static final String SERVER_IP = "192.168.1.48";
	private static Scanner scanner = new Scanner(System.in);
	private static String name = null;

	public static void main(String[] args) {

		input();

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
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

			// 이름 자체도 encoding해서 :가 들어가도 제대로 생성되도록 한다.
			pr.println("join:" + ChatWindow.encodeBase64(name));
			String[] responseToken = br.readLine().split(":");
			
			// 중복 닉네임 검사
			while("fail".equals(responseToken[1])) {
				System.out.println("이미 존재하는 대화명입니다.");
				input();
				pr = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true); // auto flush => true
				br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

				// 이름 자체도 encoding해서 :가 들어가도 제대로 생성되도록 한다.
				pr.println("join:" + ChatWindow.encodeBase64(name));
				responseToken = br.readLine().split(":");
			}
			System.out.println(name + "님, 채팅방에 입장합니다.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//scanner.close();

		new ChatWindow(socket, name).show();
	}

	// 입력 대기를 한다. 기본적으로 대화명이 비어있는지 아닌지는 검사한다.
	public static void input() {
		while(true) {
			System.out.println("대화명을 입력하세요.");
			System.out.print(">>> ");
			name = scanner.nextLine();

			if (name.isEmpty()) {
				System.out.println("대화명은 한글자 이상 입력해야 합니다.\n");
				continue;
			}
			break;
		}
	}
}
