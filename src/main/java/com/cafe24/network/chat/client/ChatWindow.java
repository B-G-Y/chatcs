package com.cafe24.network.chat.client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChatWindow {

	private Frame frame;
	private Panel pannel;
	private Button buttonSend;
	private TextField textField;
	private TextArea textArea;

	private String name;
	private Socket socket;

	public ChatWindow(Socket socket, String name) {
		this.name = name;

		frame = new Frame(name);
		pannel = new Panel();
		buttonSend = new Button("Send");
		textField = new TextField();
		textArea = new TextArea(30, 80);

		this.socket = socket;
	}

	public void show() {
		// Button
		buttonSend.setBackground(Color.GRAY);
		buttonSend.setForeground(Color.WHITE);
		buttonSend.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent actionEvent ) {
				sendMessage();
			}
		});

		// Textfield
		textField.setColumns(80);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				char keyCode = e.getKeyChar();
				if(keyCode == KeyEvent.VK_ENTER) {
					sendMessage();
				}
				super.keyPressed(e);
			}
		});

		// Pannel
		pannel.setBackground(Color.LIGHT_GRAY);
		pannel.add(textField);
		pannel.add(buttonSend);
		frame.add(BorderLayout.SOUTH, pannel);

		// TextArea
		textArea.setEditable(false);
		frame.add(BorderLayout.CENTER, textArea);

		// Frame
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				finish();
			}
		});
		frame.setVisible(true);
		frame.pack();
		
		// 서버로부터 받아온 내용을 읽는 thread를 시작한다.
		new ChatClientReceiveThread(socket).start();
	}

	private void finish() {
		System.out.println("...........");
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			pw.println("quit:" + name);
			pw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 소켓 정리 여기서 해줘야 함
		try {
			if(socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	// 채팅창 업데이트
	private void updateTextArea(String message) {
		textArea.append(message);
		textArea.append("\n");
	}

	private void sendMessage() {
		String message = textField.getText();
		// 원본 메시지는 Base64로 인코딩하여 다른 문자 처리를 수월하게 한다.
		String encodedMessage = new String(Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)));

		try {	// 소켓 받은 것을 이용하여 PrintWriter로 Server에 보낸다.
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			pw.println("message:" + encodedMessage);
			pw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		textField.setText("");	// 채팅 보낸 후 TextField 비워줌
		textField.requestFocus();

		// test
//		updateTextArea(message);
	}

	class ChatClientReceiveThread extends Thread {
		private BufferedReader br;
		private Socket socket;

		public ChatClientReceiveThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			while (true) {
				try {
					br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));

					String data = br.readLine();
					textArea.append(data + "\n");
				} catch (IOException e) {
					// socket이 열려 있을 때만 입력 대기를 하도록 한다.
					// 따라서, 예외 사항이 있을 경우 무한 루프를 빠져나온다.
					break;
				}
			}
		}
	}
}