package com.secchat.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.secchat.client.ClientData;
import com.secchat.model.User;
import com.secchat.model.UserChannel;

public class DebugPanel {
	JPanel panel = new JPanel();
	JScrollPane scrollPane = new JScrollPane(panel);
	JPanel mainPanel = new JPanel();
	private ClientData data;

	public DebugPanel() {
		this(null);
	}

	public DebugPanel(ClientData data) {
		this.data = data;

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(400, 80));

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setAutoscrolls(true);
        scrollPane.setPreferredSize(new Dimension(400, 50));

        JButton dataBtn = new JButton("Data");
        dataBtn.addActionListener(e -> onDataClicked());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(Box.createGlue(), BorderLayout.CENTER);
        topBar.add(dataBtn, BorderLayout.EAST);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(topBar);

        JPanel scrollRow = new JPanel();
        scrollRow.setLayout(new BoxLayout(scrollRow, BoxLayout.X_AXIS));
        scrollRow.add(Box.createHorizontalStrut(5));
        scrollRow.add(scrollPane);
        scrollRow.add(Box.createHorizontalStrut(5));
        mainPanel.add(scrollRow);
	}

	public void setData(ClientData data) {
		this.data = data;
	}

	private void onDataClicked() {
		if (data == null) {
			addText("No ClientData available.");
			return;
		}

		User user = data.getUser();
		if (user != null) {
			addText("User: userId=" + user.getUserId()
					+ "  name=" + user.getName()
					+ "  authToken=" + user.getAuthToken());
		} else {
			addText("User: (none)");
		}

		List<UserChannel> channels = data.loadUserChannels();
		if (channels == null || channels.isEmpty()) {
			addText("UserChannels: (none)");
		} else {
			for (UserChannel ch : channels) {
				User to = ch.getToUser();
				addText("Channel: toUserId=" + (to != null ? to.getUserId() : "?")
						+ "  toName=" + (to != null ? to.getName() : "?")
						+ "  encKey=" + ch.getPrivateEncKey());
			}
		}
	}
	
	void addText(String text) {
		text = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())
				+ "\t" + text;
		
		JTextPane txtPiece = createTextPiece(text);
		
		panel.add(Box.createVerticalStrut(10), 0);
		panel.add(txtPiece, 0);
		panel.revalidate();
		panel.repaint();
	}

	public static JTextPane createTextPiece(String text) {
		JTextPane txtPiece = new JTextPane();
		txtPiece.setContentType("text/html");
		txtPiece.setEditable(false);
		txtPiece.setBackground(null); 
		txtPiece.setBorder(null);
		txtPiece.setText(text);
		return txtPiece;
	}
	
	public Component getUI() {
        return mainPanel;
	}
}
