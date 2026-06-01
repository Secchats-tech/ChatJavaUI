package com.secchat.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.secchat.client.Client;
import com.secchat.client.ClientData;
import com.secchat.control.UIController;

class SpacedTabbedPaneUI extends BasicTabbedPaneUI {
	@Override
	protected LayoutManager createLayoutManager() {
		return new BasicTabbedPaneUI.TabbedPaneLayout() {
			@Override
			protected void calculateTabRects(int tabPlacement, int tabCount) {
				final int spacer = 20; // should be non-negative
				final int indent = 4;

				super.calculateTabRects(tabPlacement, tabCount);

				for (int i = 0; i < rects.length; i++) {
					rects[i].x += i * spacer + indent;
				}
			}
		};
	}
}

public class ChatUI {
	public static final int HEIGHT = 700;
	public static final int WIDTH = 480;

	private JFrame frame;
	private UsersView usersView;
	private ChatView chatView;
	private UIController userController;

	public ChatUI(Client client, ClientData data) {		
		frame = new JFrame("My Chat");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		
		JTabbedPane mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		mainTabbedPane.setUI(new SpacedTabbedPaneUI());
		mainTabbedPane.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (mainTabbedPane.getSelectedIndex() == 0) {
				} else if (mainTabbedPane.getSelectedIndex() == 2) {
					System.out.println("\n\nDEBUG:");
					data.listAllData();
				} else {				
					chatView.setFocus();
				}
			}
		});
		
		mainTabbedPane.setBackground(Color.BLACK);
		mainTabbedPane.setForeground(Color.WHITE);
	    Font font = new Font("Verdana", Font.CENTER_BASELINE, 18);
	    mainTabbedPane.setFont(font);
		
	    usersView = new UsersView();
	    usersView.setDebugData(data);
	    mainTabbedPane.add("Friends", usersView.getUserView());
		
		chatView = new ChatView();
		mainTabbedPane.add("Chat", chatView.getChatPanel());
		
		mainTabbedPane.add("Debug",new JPanel());
		
		frame.add(mainTabbedPane);
		userController = new UIController(client, data, usersView, chatView, frame, mainTabbedPane);
		usersView.setController(userController);
		chatView.setController(userController);
		client.addChatListener(userController);
		
		frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
            	System.out.println("Closing...");
            	userController.stopPollingThread();
            	System.exit(0);
            }
        });
	}

	public UIController getUserControler() {
		return userController;
	}
	
	public void show(){
		frame.setVisible(true);	
	}
}
