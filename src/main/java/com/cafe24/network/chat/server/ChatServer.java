package com.cafe24.network.chat.server;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ChatServer {
	public static final int PORT = 5000;

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		//List<Writer> listWriters = new ArrayList<Writer>();
		HashMap<String, Writer> writersHashMap = new HashMap<>();

		try {
			// 1. 서버 소켓 생성
			serverSocket = new ServerSocket();

			// 2. 바인딩
			serverSocket.bind(new InetSocketAddress("0.0.0.0", PORT));

			while (true) {
				// 3. 요청 대기
				Socket socket = serverSocket.accept();
				new ChatServerReceiveThread(socket, writersHashMap).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null && !serverSocket.isClosed()) {
					serverSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void log(String string) {
		System.out.println("Chat Server: " + string);
	}
}
