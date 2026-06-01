package com.secchat.control;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import com.secchat.client.ChatListener;
import com.secchat.client.Client;
import com.secchat.client.ClientData;
import com.secchat.model.Constants;
import com.secchat.model.DHSession;
import com.secchat.model.EmojiChange;
import com.secchat.model.EmojiCounter;
import com.secchat.model.FriendRequest;
import com.secchat.model.Group;
import com.secchat.model.GroupNotifications;
import com.secchat.model.Message;
import com.secchat.model.Notifications;
import com.secchat.model.User;
import com.secchat.model.UserChannel;
import com.secchat.ui.ChatView;
import com.secchat.ui.Emoji.EMOJIS;
import com.secchat.ui.Emoji.EmojiPickedListener;
import com.secchat.ui.UsersView;

public class UIController implements ChatListener, EmojiPickedListener {
	private final UsersView userView;
	private final ChatView chatView;
	private final ClientData data;
	private final JFrame frame;
	private JTabbedPane mainTabbedPane;
	private final Client client;
	private Thread pollingThread;
	private AtomicBoolean running = new AtomicBoolean(true);
	private String currentToUserID;
	private String currentToGroupID;

	public UIController(Client client, ClientData data, UsersView userView, ChatView chatView, JFrame frame,
			JTabbedPane mainTabbedPane) {
		this.userView = userView;
		this.mainTabbedPane = mainTabbedPane;
		this.data = data;
		this.frame = frame;
		this.client = client;
		this.chatView = chatView;
	}

	public void init() {
		if (data.getUser() == null || data.getUser().getUserId() == null) {
			String name = null;
			while ((name = userView.getRegisterUserDlg(frame)) == null || name.trim().length() == 0) {
			}
			frame.setTitle("User: " + name);
			userView.addDebugText("Registering user: " + name + "...");
			try {
				User user = client.registerNewUser(name);
				data.setUser(user);
				frame.setTitle("User: " + user.getName() + " ID: " + user.getUserId());
				userView.addDebugText("User: " + name + " registered on the server with ID: " + user.getUserId());
			} catch (java.net.ConnectException ex) {
				userView.addDebugText("Cannot connect to: " + client.getServerSpec());
				frame.setTitle("Error: could not register user.");
				return;
			} catch (Exception e) {
				e.printStackTrace();
				userView.addDebugText("Error, cannot register user: " + e.getClass().getCanonicalName());
				frame.setTitle("Error: could not register user.");
				return;
			}
		} else {
			frame.setTitle("User: " + data.getUser().getName() + " ID: " + data.getUser().getUserId());
			userView.addDebugText("Loaded user: " + data.getUser().getUserId());		
			loadData();
		}

		if (data.getUser() != null && data.getUser().getUserId() != null) {
			startPollingThread();
		}
	}

	private void loadData() {
		for (UserChannel uChannel : data.loadUserChannels()) {
			userView.addFriend(uChannel);
			userView.addDebugText("Loaded friend: " + uChannel.getToUser().getUserId());
		}
		
		for (FriendRequest fr : data.loadSentFriendRequests(data.getUser().getUserId())) {
			if (data.getUserChannelFor(fr.getToId()) != null) {
				System.out.println("Deleting old sent FR: " + fr.getToId());
				data.deleteSentFriendRequest(fr);
				continue;
			}
			DHSession dHSession = DHSession.deserialize(fr.getCryptoData());
			if (dHSession == null) {
				userView.addDebugText("Cannot restore friend request from DB: " + fr.getFromUserId());
				continue;
			}
			data.friendRequests.put(fr.getToId(), dHSession);
			userView.startFriendRequest(fr.getToId());
		}
		
		for (FriendRequest fr : data.loadReceivedFriendRequests(data.getUser().getUserId())) {
			if (data.getUserChannelFor(fr.getToId()) != null) {
				System.out.println("Deleting old received FR: " + fr.getToId());
				data.deleteReceivedFriendRequest(fr);
				continue;
			}
			data.receivedFriendRequests.put(fr.getFromUserId(), fr);
			userView.addReceivedFriendRequest(frame, fr.getFromUserId());
		}

		for (Group grp : data.loadGroups()) {
			userView.addGroup(grp);
			userView.addDebugText("Loaded group: " + grp.getGroupId());
		}
		
		for (Group group : data.loadGroupInvite(data.getUser())) {
			if (data.groups.containsKey(group.getGroupId())) {
				System.out.println("Deleting old group invite: " + group.getName());
				data.deleteGroupInvite(group.getGroupId());
				continue;
			}
			
			userView.addGroupInvite(frame, group);
		}
	}

	private void startPollingThread() {
		running.set(true);
		final Runnable run = new Runnable() {

			@Override
			public void run() {
				while (running.get()) {
					try {				
						System.out.println("Asking for notifications....");
						Notifications notifs = client.getNotifications(data.getUser());
						client.processNotifications(data.getUser(), notifs);
						GroupNotifications gNotifs = client.getGroupNotifications(data.getUser(), 
								new ArrayList<Group>(data.getGroups()));
						client.processGroupNotifications(data.getUser(), gNotifs);
						
						System.out.println("Long polling....");
						String messageFrom = client.longPolling(data.getUser(), 
								new ArrayList<Group>(data.getGroups()));
						if (messageFrom == null) {
							Thread.sleep(1000);
							continue;
						} else {
							System.out.println("Got polling message from: " + messageFrom);
						}
					} catch (ConnectException ex) {
						javax.swing.SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								userView.addDebugText("Connection lost.");
								chatView.addDebugText("Connection lost.");
							}
						});
						try {
							Thread.sleep(5000);
						} catch (InterruptedException ignored) {}
					} catch (IllegalArgumentException | IllegalAccessException | IOException e) {
						e.printStackTrace();
					} catch (InterruptedException ignored) {}
				}
				System.out.println("Thread stopped.");
				System.out.flush();
			}
		};
		pollingThread = new Thread(run);
		pollingThread.start();
	}

	public void wakePollingThread() {
		if (pollingThread == null) {
			return;
		}
		synchronized (running) {
			running.notifyAll();
		}
		pollingThread.interrupt();
	}

	public void stopPollingThread() {
		if (pollingThread == null) {
			return;
		}
		synchronized (running) {
			running.set(false);
			running.notifyAll();
		}
		pollingThread.interrupt();
	}

	public void inviteFriend() {
		if (data.getUser() == null || data.getUser().getUserId() == null) {
			userView.errorDlg(frame, "You are not registered yet.");
			return;
		}

		String userID;
		if ((userID = userView.inviteUserDlg(frame)) == null || userID.trim().length() == 0
				|| userID.trim().equals(data.getUser().getUserId())) {
			userView.errorDlg(frame, "Wrong user ID.");
			return;
		}


		try {
			client.friendRequest(data.getUser(), userID);
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					userView.startFriendRequest(userID);
				}
			});
		} catch (IllegalAccessException | IOException | InterruptedException e) {
			e.printStackTrace();	
		} catch (IllegalArgumentException e) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					userView.errorDlg(frame, "Cannot invite friend: " + e.getMessage());
				}
			});
		}
	}

	@Override
	public void onFriendResponse(UserChannel channel) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.out.println(channel.getToUser().getUserId());
				userView.endFriendRequest(channel.getToUser().getUserId());
			}
		});
	}

	@Override
	public void onMessageReceived(Message message) {
		final ImageIcon[] image = new ImageIcon[1];
		if (Constants.MSG_TYPE_TXT.equals(message.getType())) {
			if (!data.saveTextMessage(message)) {
				chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
			}
		} else if (Constants.MSG_TYPE_EMOJI.equals(message.getType())) {
			EmojiCounter eCounter = new EmojiCounter(message.getEmojiChange().replyToMessageId);
			eCounter.apply(message.getEmojiChange());
			if (!data.saveEmojiMessage(eCounter)) {
				chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
			}
		} else if (Constants.MSG_TYPE_IMG.equals(message.getType())) {
			chatView.addDebugText("Got image of size: " + message.getContentLength());
			image[0] = new ImageIcon(Base64.getDecoder().decode(message.getContent()));
			if (!data.saveImageMessage(message)) {
				chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
			}
		}

		if (!Constants.equals(data.getUserChannelFor(message.getFromUserId()), message.getFromName())
				&& data.updateFriendName(message.getFromUserId(), message.getFromName())) {
			updateChannelName(message);
		}

		if (currentToUserID == null || !currentToUserID.equals(message.getFromUserId())
				|| mainTabbedPane.getSelectedIndex() != 1) {
			notifyNewMessages(message.getFromUserId());
			return;
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (chatView) {
					if (Constants.MSG_TYPE_TXT.equals(message.getType())) {
						chatView.addReceivedMessage(message.getUser(), message.getMessageId(), message.getContent(),
								message.getTimestamp());
					} else if (Constants.MSG_TYPE_EMOJI.equals(message.getType())) {
						chatView.addDebugText(message.getEmojiChange().toString());
						EmojiCounter eCounter = new EmojiCounter(message.getEmojiChange().replyToMessageId);
						eCounter.apply(message.getEmojiChange());
						chatView.updateEmojis(message.getEmojiChange().replyToMessageId, eCounter, false, true);

					} else if (Constants.MSG_TYPE_IMG.equals(message.getType())) {
						chatView.addImage(message.getUser(), image[0], message.getMessageId(), true, 
								message.getTimestamp());
					} else {
						chatView.addDebugText("unexpected: " + message.getType());
					}
				}
			}
		});
	}

	private void notifyNewMessages(String userId) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userView.setFriendAlert(userId, true);
			}
		});
	}

	private void updateChannelName(Message message) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userView.updateFriendName(message.getFromUserId(), message.getFromName());
				if (message.getFromUserId().equals(currentToUserID)) {
					mainTabbedPane.setTitleAt(1, data.getChannelName(currentToUserID));
					mainTabbedPane.revalidate();
				}
			}
		});
	}

	@Override
	public boolean onFriendRequest(FriendRequest request) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userView.addReceivedFriendRequest(frame, request.getFromUserId());
			}
		});
		return true;
	}

	@Override
	public boolean onGroupRequest(String fromUserId, Group group) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userView.addGroupInvite(frame, group);
			}
		});
		return true;
	}

	public void onFriendClicked(String userID) {
		final List<Message> msgs = data.loadMessages(data.getUser().getUserId(), userID);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (chatView) {
					chatView.showGroupInvButton(false);
					currentToUserID = userID;
					currentToGroupID = null;
					mainTabbedPane.setSelectedIndex(1);
					mainTabbedPane.setTitleAt(1, data.getChannelName(userID));
					chatView.setRecipient("Friend ID: " + userID);

					chatView.clearMessagePanel();
					for (Message msg : msgs) {
						if (msg.getType().equals(Constants.MSG_TYPE_TXT)) {
							chatView.addText(msg.getUser(), msg.getMessageId(), msg.getContent(),
									msg.getToId().equals(data.getUser().getUserId()), msg.getTimestamp());
						} else if (msg.getType().equals(Constants.MSG_TYPE_IMG)) {
							ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(msg.getContent()));
							BufferedImage newBi = null;
							try {
								newBi = ImageIO.read(input);
							} catch (IOException e) {
								e.printStackTrace();
								continue;
							}
							Icon icon = new ImageIcon(newBi);
							
							chatView.addImage(msg.getUser(), icon, msg.getMessageId(), 
									msg.getToId().equals(data.getUser().getUserId()), msg.getTimestamp());
						}
						
						EmojiCounter eCounter = EmojiCounter.deserialize(msg.getOldEmoji(), msg.getMessageId());
						if (eCounter != null) {
							boolean isReceived = !data.getUser().getUserId().equals(msg.getFromUserId());
							chatView.updateEmojis(msg.getMessageId(), eCounter, isReceived, !isReceived);
						}
					}
					mainTabbedPane.revalidate();
					userView.setFriendAlert(userID, false);
				}
			}
		});
	}

	public void sendTextMessage(String text) {
		if (currentToUserID != null && data.getUserChannelFor(currentToUserID) != null) {
			Message msg = Message.createTextMessage(data.getUser(), currentToUserID, ClientData.createUID(text));
			msg.setContent(text);
			if (!data.saveTextMessage(msg)) {
				chatView.addDebugText("Cannot save message: " + msg.getMessageId());
				return;
			}

			try {
				String enc_text = data.createEncTextMessage(currentToUserID, data.getUser(), text, msg.getMessageId());
				client.postMessage(currentToUserID, data.getUser(), enc_text);
			} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
				chatView.addDebugText("Cannot send message: " + e.getClass().getCanonicalName());
				return;
			}
			chatView.addMessage(data.getUser(), msg.getMessageId(), text, msg.getTimestamp());
		} else if (currentToGroupID != null && data.getGroupFor(currentToGroupID) != null) {
			Group group = data.getGroupFor(currentToGroupID);
			try {
				String enc_text = data.createGroupMessage(group, data.getUser(), text);
				client.postGroupMessage(group, data.getUser(), enc_text);
			} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
				chatView.addDebugText("Cannot send message: " + e.getClass().getCanonicalName());
				return;
			}
			wakePollingThread();
			chatView.resetFocus();
		} else {
			chatView.addDebugText("Please select a group or friend first.");
			return;
		}
	}

	public void sendImage(BufferedImage buffered) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			ImageIO.write(buffered, "png", output);
		} catch (IOException e) {
			chatView.addDebugText("Cannot send image: " + e.getClass().getCanonicalName());
			return;
		}
		String content = new String(Base64.getEncoder().encode(output.toByteArray()));
		Icon icon = new ImageIcon(buffered);
		output = null;
		
		if (currentToUserID != null && data.getUserChannelFor(currentToUserID) != null) {			
			sendImageToFriend(content, icon);
			return;
		}  
		
		if(currentToGroupID != null && data.getGroupFor(currentToGroupID) != null) {
			sendImageToGroup(content, icon);	
			return;
		}
		
		chatView.addDebugText("Please select a group or friend first.");
		return;
	}

	private void sendImageToGroup(String content, Icon icon) {
		Group group = data.getGroupFor(currentToGroupID);
		try {
			String enc_img = data.createEncGroupMessage(group, data.getUser(), content);
			client.postGroupMessage(group, data.getUser(), enc_img);
			wakePollingThread();
			chatView.resetFocus();
		} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
			chatView.addDebugText("Cannot send image: " + e.getClass().getCanonicalName());
			e.printStackTrace();
		}
	}

	private void sendImageToFriend(String content, Icon icon) {
		Message message = Message.createImageMessage(data.getUser(), currentToUserID, ClientData.createUID(content));
		message.setContent(content);
		if (!data.saveImageMessage(message)) {
			chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
		}

		try {
			String enc_img = data.createEncImageMessage(currentToUserID, data.getUser(), content, message.getMessageId());
			client.postMessage(currentToUserID, data.getUser(), enc_img);
			chatView.addImage(data.getUser(), icon, message.getMessageId(), false, Constants.getTimestamp());
		} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
			chatView.addDebugText("Cannot send image: " + e.getClass().getCanonicalName());
			e.printStackTrace();
		}
	}

	public void createGroup() {
		String name = userView.getRegisterGroupDlg(frame);
		if (name == null || name.trim().length() == 0) {
			return;
		}

		try {
			Group group = client.registerNewGroup(name, Constants.ALGO_AES_256);
			if (!data.saveGroup(group)) {
				throw new IOException("Failed to save group.");
			}
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					userView.addGroup(group);
				}
			});
		} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
			chatView.addDebugText("Cannot register group: " + e.getClass().getCanonicalName());
			return;
		}
	}

	public void onGroupClicked(final String groupId) {
		final List<Message> msgs = data.loadMessages(groupId);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (chatView) {
					chatView.showGroupInvButton(true);
					chatView.setRecipient("ID: " + groupId);
					currentToGroupID = groupId;
					currentToUserID = null;
					mainTabbedPane.setSelectedIndex(1);
					mainTabbedPane.setTitleAt(1, "Group " + data.getGroupFor(groupId).getName());

					chatView.clearMessagePanel();
					for (Message msg : msgs) {
						final boolean isOwnMessage = msg.getFromUserId().equals(data.getUser().getUserId());						
						if (msg.getType().equals(Constants.MSG_TYPE_TXT)) {
							chatView.addText(msg.getUser(), msg.getMessageId(), msg.getContent(), !isOwnMessage,
									msg.getTimestamp());
						} else if (msg.getType().equals(Constants.MSG_TYPE_IMG)) {
							ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(msg.getContent()));
							BufferedImage newBi = null;
							try {
								newBi = ImageIO.read(input);
							} catch (IOException e) {
								e.printStackTrace();
								continue;
							}
							Icon icon = new ImageIcon(newBi);
							chatView.addImage(msg.getUser(), icon, msg.getMessageId(), !isOwnMessage,
									msg.getTimestamp());
						}
						
						EmojiCounter eCounter = EmojiCounter.deserialize(msg.getOldEmoji(), msg.getMessageId());
						if (eCounter != null) {
							System.out.println("Emojis: " + eCounter.serialize());
							chatView.updateEmojis(msg.getMessageId(), eCounter, !isOwnMessage, true);
						}
					}

					userView.setGroupAlert(currentToGroupID, false);
				}
			}
		});
	}

	@Override
	public void onGroupMessageReceived(Group group, Message message) {
		final EmojiCounter[] eCounter = new EmojiCounter[1];
		final ImageIcon[] image = new ImageIcon[1];
		final boolean isOwnMessage = data.getUser().getUserId().equals(message.getFromUserId());

		if (Constants.MSG_TYPE_TXT.equals(message.getType())) {
			if (!data.saveTextMessage(message)) {
				chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
			}
		} else if (Constants.MSG_TYPE_EMOJI.equals(message.getType())) {

			eCounter[0] = data.loadEmoji(message.getEmojiChange().replyToMessageId);
			if (!isOwnMessage) {
				eCounter[0].apply(message.getEmojiChange());
				if (!data.saveEmojiMessage(eCounter[0])) {
					chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
				}
			}
		} else if (Constants.MSG_TYPE_IMG.equals(message.getType())) {
			chatView.addDebugText("Got image of size: " + message.getContentLength());
			image[0] = new ImageIcon(Base64.getDecoder().decode(message.getContent()));
			if (!data.saveImageMessage(message)) {
				chatView.addDebugText("Cannot locally save msg from: " + message.getFromName());
			}
		}

		if (currentToGroupID == null || !currentToGroupID.equals(message.getToId())
				|| mainTabbedPane.getSelectedIndex() != 1) {
			notifyNewGroupMessages(message.getToId());
			return;
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (chatView) {
					if (Constants.MSG_TYPE_TXT.equals(message.getType())) {
						chatView.addText(message.getUser(), message.getMessageId(), message.getContent(),
								!message.getFromUserId().equals(data.getUser().getUserId()), message.getTimestamp());
					} else if (Constants.MSG_TYPE_EMOJI.equals(message.getType())) {
						chatView.addDebugText(message.getEmojiChange().toString());
						chatView.updateEmojis(message.getEmojiChange().replyToMessageId, eCounter[0], false, true);
					} else if (Constants.MSG_TYPE_IMG.equals(message.getType())) {
						chatView.addImage(message.getUser(), image[0], message.getMessageId(), 
								!message.getFromUserId().equals(data.getUser().getUserId()), message.getTimestamp());
					} else {
						chatView.addDebugText("unexpected: " + message.getType());
					}
				}
			}
		});
	}

	private void notifyNewGroupMessages(String userId) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				userView.setGroupAlert(userId, true);
			}
		});
	}

	public void groupInvite() {
		Group group;
		if (data.getUserChannels().size() == 0 || currentToGroupID == null
				|| (group = data.getGroupFor(currentToGroupID)) == null) {
			return;
		}

		String[] users = new String[data.getUserChannels().size()];
		int indx = 0;
		for (UserChannel uChannel : data.getUserChannels()) {
			users[indx++] = uChannel.getToUser().getName() + " " + uChannel.getToUser().getUserId();
		}

		URL imgURL = getClass().getResource("/images/grp_invite.png");
		ImageIcon icon = new ImageIcon(imgURL);

		String userId = (String) JOptionPane.showInputDialog(frame, "Invite friend", "Group Invite",
				JOptionPane.INFORMATION_MESSAGE, icon, users, users[0]);
		if (userId == null) {
			return;
		}
		userId = userId.substring(userId.lastIndexOf(" ") + 1);

		final String[] msg = new String[1];
		try {
			client.groupInvite(data.getUser(), userId, group);
			msg[0] = "Group invitation sent to " + userId + " for group " + group.getName();
		} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
			msg[0] = "Error on invite: " + e.getClass().getCanonicalName();
		}
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatView.addDebugText(msg[0]);
			}
		});
	}

	@Override
	public void onEmojiPicked(long targetMessageId, EMOJIS newEmoji, EMOJIS oldEmoji) {
		if (currentToUserID != null && data.getUserChannelFor(currentToUserID) != null) {
			try {
				System.out.println("onEmojiPicked: " + targetMessageId + " : " + newEmoji + " : " + oldEmoji);
				EmojiCounter eCounter = new EmojiCounter(targetMessageId);
				eCounter.apply(new EmojiChange(targetMessageId, newEmoji.name(), oldEmoji.name()));
				if (!data.saveEmojiMessage(eCounter)) {
					chatView.addDebugText("Cannot save emoji: " + newEmoji);
					return;
				}

				String enc_text = data.createNewEmojiMessage(currentToUserID, data.getUser(), targetMessageId,
						newEmoji.name(), oldEmoji.name());
				client.postMessage(currentToUserID, data.getUser(), enc_text);
			} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
				chatView.addDebugText("Cannot send message: " + e.getClass().getCanonicalName());
				return;
			}
		} else if (currentToGroupID != null && data.getGroupFor(currentToGroupID) != null) {
			try {
				System.out.println("onEmojiPicked: " + targetMessageId + " : " + newEmoji + " : " + oldEmoji);
				EmojiCounter eCounter = data.loadEmoji(targetMessageId);
				eCounter.apply(new EmojiChange(targetMessageId, newEmoji.name(), oldEmoji.name()));
				if (!data.saveEmojiMessage(eCounter)) {
					chatView.addDebugText("Cannot save emoji: " + newEmoji);
					return;
				}

				String enc_text = data.createNewGroupEmojiMessage(currentToGroupID, data.getUser(), targetMessageId,
						newEmoji.name(), oldEmoji.name());

				client.postGroupMessage(data.getGroupFor(currentToGroupID), data.getUser(), enc_text);
			} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException e) {
				chatView.addDebugText("Cannot send message: " + e.getClass().getCanonicalName());
				return;
			}
		} else {
			chatView.addDebugText("Please select a group or friend first.");
			return;
		}
	}

	public boolean respondToFriendRequest(String fromUserId) {
		try {
			FriendRequest fr = data.receivedFriendRequests.get(fromUserId);
			if (fr == null) {
				System.out.println("Cannot find friend request: " + fromUserId);
				return false;
			}
			client.processFriendRequest(data.getUser(), fr);
			data.receivedFriendRequests.remove(fromUserId);
			if (!data.deleteReceivedFriendRequest(fr)) {
				System.out.println("Cannot delete friend request: " + fromUserId);
			}
			return true;
		} catch (IllegalArgumentException | IllegalAccessException | IOException | InterruptedException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void removeReceivedFriendRequest(String fromUserId) {
		FriendRequest fr = data.receivedFriendRequests.get(fromUserId);
		if (fr != null) {
			if (!data.deleteReceivedFriendRequest(fr)) {
				System.out.println("Cannot delete friend request: " + fromUserId);
			}
			data.receivedFriendRequests.remove(fromUserId);
		}
	}

	public void removeGroupInvite(String groupId) {
		if (!data.deleteGroupInvite(groupId)) {
			System.out.println("Cannot delete friend request: " + groupId);
		}
	}
	
	public void listAllData() {
		data.listAllData();
	}

	public boolean addGroup(Group group) {
		if (!data.saveGroup(group)) {
			return false;
		}
		data.groups.put(group.getGroupId(), group);
		return true;
	}
}
