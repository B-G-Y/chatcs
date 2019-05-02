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

import com.cafe24.network.chat.server.ChatServer;

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
		System.out.println(name + "님, 채팅을 종료합니다.");
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			pw.println("quit:" + name);
			//pw.flush();	// flush를 사용할 경우 퇴장 메시지가 두 번 broadcast된다.
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

	// 채팅창 업데이트(사용 안함)
	private void updateTextArea(String message) {
		textArea.append(message);
		textArea.append("\n");
	}

	private void sendMessage() {
		String message = textField.getText();
		
		// 채팅 보낸 후 TextField 비워줌. 귓속말 사용할 때도 적용시키기 위해 앞에 작성함
		textField.setText("");
		textField.requestFocus();
		
		if(message.length() > 3 && "/w ".equals(message.substring(0, 3))) {	// 귓속말 명령이라고 인식한다.
			// 인자가 충분하지 못하다면 오류 메시지 없이 sendMessage 해버린다.
			String[] whisperMessage = message.split(" ");
			if(whisperMessage.length >= 3) {
				whisperMessage(message);
				return;
			}
		}
		
		sendMessageToServer("message:" + encodeBase64(message));

		// test
//		updateTextArea(message);
	}

	// 귓속말 처리
	private void whisperMessage(String data) {
		data = data.substring(3);
		int blankIndex = data.indexOf(" ");
		String receiverName = data.substring(0, blankIndex);
		String message = data.substring(blankIndex + 1);

		// 이름이 깨지는 문제 때문에 이름도 메시지와 같이 UTF-8->Base64로 인코딩함 
		sendMessageToServer("whisper:" + encodeBase64(name) + ":" + encodeBase64(receiverName) + ":" + encodeBase64(message));
	}
	
	// encode할 것이 많아져 별도의 메서드로 분리
	private String encodeBase64(String data) {
		return new String(Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8)));
	}
	
	// 소켓 받은 것을 이용하여 PrintWriter로 Server에 보낸다.
	// Server에게 message를 출력시킨다. 코드 중복 기능을 피하기 위해 별도의 메서드로 분리하였다.
	private void sendMessageToServer(String message) {
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			// 원본 메시지는 Base64로 인코딩하여 다른 문자 처리를 수월하게 한다.
			pw.println(message);
			pw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
					br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

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