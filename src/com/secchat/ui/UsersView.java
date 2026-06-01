package com.secchat.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.secchat.client.ClientData;
import com.secchat.control.UIController;
import com.secchat.model.Group;
import com.secchat.model.UserChannel;

public class UsersView {
	private static final ImageIcon FRIEND_REQ_ICON = new ImageIcon(UsersView.class.getResource("/images/friend_req.png"));
	private static final ImageIcon USER_ICON = new ImageIcon(UsersView.class.getResource("/images/user.png"));
	private static final ImageIcon BELL_ICON = new ImageIcon(UsersView.class.getResource("/images/bell.png"));
	private static final ImageIcon GROUP_ICON = new ImageIcon(UsersView.class.getResource("/images/group.png"));
	private static final ImageIcon EXCLAMATION_ICON = new ImageIcon(UsersView.class.getResource("/images/exclamation.png"));
	private static final ImageIcon GRP_INV_ICON = new ImageIcon(UsersView.class.getResource("/images/grp_invite.png"));
	private static final ImageIcon USERS_ICON = new ImageIcon(UsersView.class.getResource("/images/users.png"));
	
	JPanel mainPanel;
	JPanel headerPanel;

	JPanel footerPanel;
	JPanel usersPanel;
	
	DebugPanel debugPanel;
	UIController controller;
	
	ConcurrentHashMap<String, JLabel> usersView = new ConcurrentHashMap<String, JLabel>();
	
	public UsersView() {
		createUsersPanel();
	}
	
	private void createUsersPanel() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		headerPanel = new JPanel();
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		
		usersPanel = new JPanel();
		usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
		
		mainPanel.add(usersPanel, BorderLayout.CENTER);
		
		
		footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
		
		
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actionPanel.add(inviteBtn());
		actionPanel.add(Box.createHorizontalStrut(10));
		actionPanel.add(createGroupBtn());
		
		footerPanel.add(actionPanel);
		
		debugPanel = new DebugPanel();
		footerPanel.add(debugPanel.getUI());
		footerPanel.add(Box.createVerticalStrut(10));
		
		mainPanel.add(footerPanel, BorderLayout.SOUTH);
	}

	private JButton createGroupBtn() {
		JButton button = new JButton("New Group");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.createGroup();
			}
		});
		
		return button;
	}

	private JButton inviteBtn() {
		JButton button = new JButton("Invite Friend");
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.inviteFriend();
			}
		});
		
		return button;
	}
	
	public Component getUserView() {
		return mainPanel;
	}
	
	public void setDebugData(ClientData data) {
		debugPanel.setData(data);
	}

	public void addDebugText(String text) {
		debugPanel.addText(text);
	}

	public String getRegisterUserDlg(JFrame frame) {
		String name = (String) JOptionPane.showInputDialog(frame,
				"No user registered for this chat.\nPlease provide a user name: ", 
				"Register New User", JOptionPane.INFORMATION_MESSAGE,
				USER_ICON, null, "");
		return name;
	}

	public String getRegisterGroupDlg(JFrame frame) {
		String name = (String) JOptionPane.showInputDialog(frame,
				"Please provide a group name: ", 
				"Register Group", JOptionPane.INFORMATION_MESSAGE,
				GROUP_ICON, null, "");
		return name;
	}
	
	public String inviteUserDlg(JFrame frame) {
		String userID = (String) JOptionPane.showInputDialog(frame,
				"Please provide your friend User ID: ", 
				"Invite Friend", JOptionPane.INFORMATION_MESSAGE,
				USERS_ICON, null, "");
		// addDebugText("Got: " + userID);
		return userID;
	}
	
	public void errorDlg(JFrame frame, String text) {
		JOptionPane.showMessageDialog(frame, text, "Error", JOptionPane.WARNING_MESSAGE, EXCLAMATION_ICON);
	}
	
	public boolean acceptFriendRequest(JFrame frame, String userId) {
		int reply = JOptionPane.showConfirmDialog(frame, 
				"Accept request from " + userId + "?", "Friend Request",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, FRIEND_REQ_ICON);
		return reply == JOptionPane.YES_OPTION;
	}

	public void setController(UIController controller) {
		this.controller = controller;
	}

	public void startFriendRequest(String userID) {
		JLabel label = new JLabel("Invited: " + userID, USER_ICON, SwingConstants.LEFT);
		
		usersView.put(userID, label);

		usersPanel.add(label);
		usersPanel.revalidate();
	}
	
	public void endFriendRequest(String userID) {
		JLabel label = usersView.get(userID);
		if (label == null) {
			addDebugText("Null label for: " + userID);
			return;
		}
		label.setText(userID);
		label.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				controller.onFriendClicked(userID);
			}
		});
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		usersPanel.revalidate();
	}
	
	public void updateFriendName(String userID, String name) {
		JLabel label = usersView.get(userID);
		if (label == null) {
			addDebugText("Null label for: " + userID);
			return;
		}
		label.setText(name);
		label.revalidate();
	}
	
	public void setFriendAlert(String userID, boolean alertOn) {
		JLabel label = usersView.get(userID);
		if (label == null) {
			addDebugText("Null label for: " + userID);
			return;
		}
		label.setIcon(alertOn ? BELL_ICON : USER_ICON);
		label.revalidate();
	}
	
	public void setGroupAlert(String userID, boolean alertOn) {
		JLabel label = usersView.get(userID);
		if (label == null) {
			addDebugText("Null label for: " + userID);
			return;
		}
		label.setIcon(alertOn ? BELL_ICON : GROUP_ICON);
		label.revalidate();
	}
	
	public void addGroupInvite(Component frame, final Group group) {
		final JLabel label = new JLabel("Group invite to: " + group.getName(),
				GRP_INV_ICON, SwingConstants.LEFT);
	
		label.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				int reply = JOptionPane.showConfirmDialog(frame, 
						"Join the group " + group.getName() + "?", 
						"Group Invite", JOptionPane.YES_NO_CANCEL_OPTION, 
						JOptionPane.QUESTION_MESSAGE, FRIEND_REQ_ICON);
				if (reply == JOptionPane.CANCEL_OPTION) {
					return;
				}
				if  (reply == JOptionPane.NO_OPTION) {
					controller.removeGroupInvite(group.getGroupId());
					usersView.remove(group.getGroupId());
					usersPanel.remove(label);
					usersPanel.revalidate();
					usersPanel.repaint();
					return;
				}
				
				if (!controller.addGroup(group)) {
					System.out.println("Cannot add group: " + group);
					return;
				}

				label.removeMouseListener(label.getMouseListeners()[0]);
				label.addMouseListener(new MouseAdapter() {

					public void mouseClicked(MouseEvent e) {
						controller.onGroupClicked(group.getGroupId());
					}
				});
				
				label.setIcon(GROUP_ICON);
				label.setText(group.getName());
			}
		});
		
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	    label.setBorder(new EmptyBorder(15, 10, 15, 10));

		usersView.put(group.getGroupId(), label);
		
		usersPanel.add(label);
		usersPanel.revalidate();
		usersPanel.repaint();
	}
	
	public void addGroup(Group group) {
		JLabel label = new JLabel(group.getName(), GROUP_ICON, SwingConstants.LEFT);
		label.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				controller.onGroupClicked(group.getGroupId());
			}
		});
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setBorder(new EmptyBorder(15, 10, 15, 10));
		
		usersView.put(group.getGroupId(), label);
		
		usersPanel.add(label);
		usersPanel.revalidate();
		usersPanel.repaint();
	}

	public void addReceivedFriendRequest(Component frame, final String fromUserId) {
		final JLabel label = new JLabel("Friend request from: " + fromUserId,
				FRIEND_REQ_ICON, SwingConstants.LEFT);
		label.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				int reply = JOptionPane.showConfirmDialog(frame, 
						"Accept request from " + fromUserId + "?", "Friend Request",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, FRIEND_REQ_ICON);
				if (reply == JOptionPane.CANCEL_OPTION) {
					return;
				}
				if  (reply == JOptionPane.NO_OPTION) {
					controller.removeReceivedFriendRequest(fromUserId);
					usersView.remove(fromUserId);
					usersPanel.remove(label);
					usersPanel.revalidate();
					usersPanel.repaint();
					return;
				}
			
				if (!controller.respondToFriendRequest(fromUserId)) {
					addDebugText("Cannot respond to friend request at the moment.");
					return;
				}

				label.removeMouseListener(label.getMouseListeners()[0]);
				addOnClickListener(fromUserId, label);
				label.setIcon(USER_ICON);
				label.setText(fromUserId);
			}
		});
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	    label.setBorder(new EmptyBorder(15, 10, 15, 10));

		usersView.put(fromUserId, label);
		
		usersPanel.add(label);
		usersPanel.revalidate();
		usersPanel.repaint();
	}
	
	private void addOnClickListener(String userID, JLabel label) {
		label.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				controller.onFriendClicked(userID);
			}
		});
	}
	
	public void addFriend(UserChannel friend) {
		JLabel label = new JLabel(
				friend.getToUser().getName() + " " + friend.getToUser().getUserId(), 
				USER_ICON, SwingConstants.LEFT);
		addOnClickListener(friend.getToUser().getUserId(), label);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setForeground(ChatView.getColor(friend.getToUser().getUserId()));
		label.setBorder(new EmptyBorder(15, 10, 15, 10));

		usersView.put(friend.getToUser().getUserId(), label);
		
		usersPanel.add(label);
		usersPanel.revalidate();
		usersPanel.repaint();
	}

	public boolean acceptGroupRequest(JFrame frame, Group group) {
		int reply = JOptionPane.showConfirmDialog(frame, 
				"Accept request from " + group + "?", "Join Group " + group.getName(),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, GRP_INV_ICON);
		return reply == JOptionPane.YES_OPTION;
	}
	
}
