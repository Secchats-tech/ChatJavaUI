package com.secchat.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import com.secchat.control.UIController;
import com.secchat.model.EmojiCounter;
import com.secchat.model.User;
import com.secchat.ui.Emoji.EmojiPickedListener;

public class ChatView {

	private static class RoundBorder extends AbstractBorder {

		private static final long serialVersionUID = 1L;
		private final Color color;
		
		public RoundBorder(Color color) {
			super();
			this.color = color;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Color oldColor = g.getColor();
			Graphics2D g2 = (Graphics2D) g;
			Stroke oldStroke = g2.getStroke();

			g2.setStroke(new BasicStroke(2.0f));
			g.setColor(color);
			g.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
			
			g2.setStroke(oldStroke);
			g.setColor(oldColor);
		}

		public Insets getBorderInsets(Component c) {
			return new Insets(15, 10, 10, 10);
		}

		public Insets getBorderInsets(Component c, Insets insets) {
			insets.left = insets.top = insets.right = insets.bottom = 4;
			return insets;
		}
	}
	
	private static class EmojiComponents {
		JPanel emojiLine;
		JButton emojiButton;
		
		public EmojiComponents(JPanel emojiLine, JButton emojiButton) {
			super();
			this.emojiLine = emojiLine;
			this.emojiButton = emojiButton;
		}
	}
	
	private static final FileNameExtensionFilter IMAGE_FILTER = new FileNameExtensionFilter("Image Files", "jpg", "png", "tif");
	private final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final Color[] colors = { Color.BLUE, Color.CYAN, Color.RED, Color.MAGENTA, Color.GREEN, Color.ORANGE, Color.PINK };
	
	private JPanel chatPanel = new JPanel();
	private DebugPanel debugPanel = new DebugPanel();
	private JLabel recipient;
	private JPanel messagePanel;
	private JTextArea inputText;
	private UIController controller;
	private JScrollPane scrollPane;
	private int index = 1;
	private AtomicBoolean autoScroll = new AtomicBoolean(true);
	private JButton grpInvBtn;
	
	private ConcurrentHashMap<Long, EmojiComponents> emojiComponent = new ConcurrentHashMap<Long, EmojiComponents>();
	
	public ChatView() {
		SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getDefault());
		createChatPanel();
	}
	
	public void setController(UIController controller) {
		this.controller = controller;
	}
	
	public void showGroupInvButton(boolean value) {
		grpInvBtn.setVisible(value);
	}
	
	public void setRecipient(String text) {
		recipient.setText(text);
	}
	
	public boolean isAutoScroll() {
		return autoScroll.get();
	}
	
	public void setAutoScroll(boolean value) {
		autoScroll.set(value);
	}
	
	public void addMessage(User from, long messageId, String message, long timestamp) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	synchronized(this) {
            		addText(from, messageId, message, false, timestamp);
	        		inputText.setText("");
	        		inputText.revalidate();
	        		inputText.grabFocus();
	        		chatPanel.revalidate();
            	}
            }
        });
	}
	
	public void resetFocus() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
        		inputText.setText("");
        		inputText.grabFocus();
        		chatPanel.revalidate();
            }
        });
	}
	
	public void addReceivedMessage(User from, long messageId, String message, long timestamp) {
		synchronized(this) {
			addText(from, messageId, message, true, timestamp);
			chatPanel.revalidate();
		}
	}

	public void addText(User from, long messageId, String text, boolean isReceived, long timestamp) {
		Component message = createMessage(from, messageId, text, isReceived, timestamp);
		messagePanel.add(message, index++);
		messagePanel.add(Box.createRigidArea(new Dimension(300, 5)), index++);
		messagePanel.revalidate();
		//scrollPane.scrollRectToVisible(message.getBounds());
	}
	
	public void clearMessagePanel() {
		messagePanel.removeAll();
		emojiComponent.clear();
		messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
		messagePanel.add(Box.createRigidArea(new Dimension(10, 500)));	
		index = 1;
	}

	private static final Pattern urlPattern = Pattern.compile(
	        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
	                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
	                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
	        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	
	private String extractURLs(String text) {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = urlPattern.matcher(text);
		
		int lastMatchedPos = 0;
		while (matcher.find()) {
		    int matchStart = matcher.start(1);
		    int matchEnd = matcher.end();
		    sb.append(text.substring(lastMatchedPos, matchStart));
		    sb.append("<a href='");
		    sb.append(text.substring(matchStart, matchEnd));
		    sb.append("'>");
		    sb.append(text.substring(matchStart, matchEnd));
		    sb.append("<a/>");
		    lastMatchedPos = matchEnd;
		}
		sb.append(text.substring(lastMatchedPos));

		return sb.toString();
	}
	
	private boolean containsURLs(String text) {
		Matcher matcher = urlPattern.matcher(text);
		return matcher.find();
	}
	
	private JTextComponent createLinks(User from, String text, boolean isReceived) {
		JEditorPane editor = new JEditorPane();
		editor.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
		editor.setEditable(false);
		editor.setBackground(null);
		editor.setBorder(new RoundBorder(isReceived ? getColor(from.getUserId()) : new Color(51, 204, 255)));
		editor.setText(from.getName() + ": " + extractURLs(text));
		editor.addHyperlinkListener(new HyperlinkListener() {
		    public void hyperlinkUpdate(HyperlinkEvent e) {
		        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		        	try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException | URISyntaxException e1) {
						e1.printStackTrace();
					}
		        }
		    }
		});

		return (JTextComponent)editor;
	}
	
	private JTextComponent createTextEntry(User from, String text, boolean isReceived) {
		JTextComponent component;
		JTextArea txtPiece = new JTextArea();
		txtPiece.setLineWrap(true);
		txtPiece.setWrapStyleWord(true);
		txtPiece.setEditable(false);
		txtPiece.setBackground(null); 
		txtPiece.setText(from.getName() + ": " + text);
		txtPiece.setBorder(new RoundBorder(isReceived ? getColor(from.getUserId()) : new Color(51, 204, 255)));
		component = txtPiece;
		return component;
	}

	private Component createMessage(User from, long messageId, String text, 
			boolean isReceived, long timestamp) {
		JPanel firstLine = new JPanel();
		firstLine.setLayout(new BoxLayout(firstLine, BoxLayout.LINE_AXIS));
		JLabel label = new JLabel(SIMPLE_DATE_FORMAT.format(timestamp));
		firstLine.add(label);

		JTextComponent component = null;
		if (containsURLs(text)) {
			component = createLinks(from, text, isReceived);
		} else {
			component = createTextEntry(from, text, isReceived);
		}
		
		label.setFont(component.getFont());
		firstLine.add(Box.createRigidArea(new Dimension(10, 1)));
		firstLine.add(component);
		firstLine.add(Box.createRigidArea(new Dimension(5, 1)));
		
		
		JButton emojiButton = null;
		if (isReceived) {
			emojiButton = createEmojiButton(messageId, controller);	
			
			firstLine.add(emojiButton);
			firstLine.add(Box.createRigidArea(new Dimension(15, 1)));
		}
		
		JPanel secondLine = new JPanel();
		secondLine.setLayout(new FlowLayout(FlowLayout.RIGHT));
		emojiComponent.put(messageId, new EmojiComponents(secondLine, emojiButton));
		
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new GridLayout(2, 1));
		wrapper.add(firstLine);
		wrapper.add(secondLine);
		
		return wrapper;
	}

	public void updateEmojis(long messageId, EmojiCounter emojiCount, boolean isReceived, boolean showEmojiLine) {
		EmojiComponents components = emojiComponent.get(messageId);
		if (components == null) {
			return;
		}
		if (isReceived) {
			updateEmojiButton(components, emojiCount);
		}
		if (!showEmojiLine || components.emojiLine == null) {
			return;
		}

		components.emojiLine.removeAll();
		for (String emoji : emojiCount.getEmojiCount().keySet()) {
			long value = emojiCount.getEmojiCount().get(emoji);
			ImageIcon icon = null;
			if (value > 0 && !Emoji.NONE_EMOJI.name().equals(emoji) 
					&& (icon = Emoji.getIcon(emoji)) != null) {
				if (value > 1) {
					components.emojiLine.add(new JLabel(String.valueOf(value), icon, SwingConstants.LEFT));
				} else {
					components.emojiLine.add(new JLabel(icon, SwingConstants.LEFT));
				}
				components.emojiLine.add(Box.createRigidArea(new Dimension(5, 1)));
			}
		}
		components.emojiLine.revalidate();
		components.emojiLine.repaint();
	}

	private void updateEmojiButton(EmojiComponents components, EmojiCounter emojiCount) {
		if (components.emojiButton == null || emojiCount == null) {
			return;
		}
		ImageIcon icon = Emoji.getIcon(emojiCount.getSelectedEmoji());
		if (icon == null) {
			System.out.println("Cannot find icon for emoji:\t" + emojiCount.getSelectedEmoji());
			return;
		}
		components.emojiButton.setIcon(icon);
		components.emojiButton.revalidate();
	}

	public static Color getColor(String userId) {
		int mod = userId.hashCode() % colors.length;
		return colors[mod >= 0 ? mod : colors.length + mod];
	}

	private static JButton createEmojiButton(long messageId, EmojiPickedListener listener) {		
		final JButton emojiButton = new JButton();
		emojiButton.setIcon(Emoji.NONE_ICON);
		emojiButton.setBorder(new LineBorder(UIManager.getColor("control")));
		
		final JPopupMenu emojiMenu = Emoji.createEmojiMenu(messageId, emojiButton, listener);
		
		emojiButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		emojiButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseExited(MouseEvent e) {
				emojiButton.setBorder(new LineBorder(UIManager.getColor("control")));
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				emojiButton.setBorder(new LineBorder(Color.RED));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				emojiMenu.show(e.getComponent(),
	                       e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				emojiMenu.show(e.getComponent(),
	                       e.getX(), e.getY());
			}
		});
		
		
		return emojiButton;
	}
	
	private void createChatPanel() {
		chatPanel.setLayout(new BorderLayout());
			
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
		recipient = new JLabel("");
		headerPanel.add(recipient);	
		JToggleButton toggleButton = new JToggleButton(
				autoScroll.get() ? "Autoscroll  on" : "Autoscroll off", autoScroll.get());
		toggleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
				autoScroll.set(abstractButton.getModel().isSelected());
				toggleButton.setText(autoScroll.get() ? "Autoscroll  on" : "Autoscroll off");
			}
		});
		headerPanel.add(Box.createHorizontalGlue());
		grpInvBtn = new JButton("Invite friend to group");
		grpInvBtn.addActionListener(inviteFriendToGroup());
		showGroupInvButton(false);
		
		headerPanel.add(grpInvBtn );
		headerPanel.add(Box.createHorizontalGlue());	
		headerPanel.add(toggleButton);
		
		chatPanel.add(headerPanel, BorderLayout.NORTH);
		
		messagePanel = new JPanel();
		messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
		messagePanel.add(Box.createRigidArea(new Dimension(10, 500)));	
		
        scrollPane = new JScrollPane(messagePanel);
		scrollPane .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setAutoscrolls(true);
        
        scrollPane.getVerticalScrollBar().addAdjustmentListener(
			e -> {
				if (autoScroll.get()) {
					e.getAdjustable().setValue(e.getAdjustable().getMaximum());  
				}
			});
		
		chatPanel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));	
		footerPanel.add(createTextInputUI());
		footerPanel.add(Box.createVerticalStrut(10));
		footerPanel.add(debugPanel.getUI());
		footerPanel.add(Box.createVerticalStrut(5));

		chatPanel.add(footerPanel, BorderLayout.SOUTH);
	}
	
	private ActionListener inviteFriendToGroup() {
		return new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.groupInvite();
			}
		};
	}

	private Component createTextInputUI() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setPreferredSize(new Dimension(400, 30));
		panel.setSize(new Dimension(400, 30));
		
		inputText = new JTextArea(3, 0);
		inputText.setLineWrap(true);
		
		JScrollPane scrollPane = new JScrollPane(inputText);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setAutoscrolls(true);
        scrollPane.setPreferredSize(new Dimension(400, 30));
	
		panel.add(new JLabel("Text: "));
		panel.add(scrollPane, BorderLayout.PAGE_START);
						
		JButton sendButton = createSendButton();
		
		panel.add(sendButton);
		panel.add(Box.createHorizontalStrut(5));
		
		JButton upldButton = createUploadButton();
		panel.add(upldButton);
		panel.add(Box.createHorizontalStrut(5));
		
		return panel;
	}

	private JButton createSendButton() {
		JButton sendButton = new JButton();
		URL imgURL = getClass().getResource("/images/send.png");
		sendButton.setIcon(new ImageIcon(imgURL));
		sendButton.setBorder(null);
		sendButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = inputText.getText();
				if (text == null || text.trim().length() == 0) {
					return;
				}
				controller.sendTextMessage(text);
			}
		});
		sendButton.addMouseListener(new MouseListener() {
			@Override
			public void mouseExited(MouseEvent e) {
				sendButton.setBorder(new LineBorder(UIManager.getColor("control")));
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				sendButton.setBorder(new LineBorder(Color.RED));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		return sendButton;
	}

	public void addImage(User user, Icon icon, long messageId, boolean isReceived, long timestamp) {		
		JPanel imgLine = new JPanel();
		imgLine.setLayout(new BoxLayout(imgLine, BoxLayout.LINE_AXIS));
		imgLine.setBackground(null); 
		
		JLabel label = new JLabel(SIMPLE_DATE_FORMAT.format(timestamp));
		imgLine.add(label);

		imgLine.add(Box.createRigidArea(new Dimension(10, 1)));
		
		JLabel lImg = new JLabel();
		lImg.setText("From: " + user.getName());
		lImg.setIcon(icon);
		lImg.setHorizontalTextPosition(JLabel.CENTER);
		lImg.setVerticalTextPosition(JLabel.BOTTOM);
		lImg.setPreferredSize(lImg.getPreferredSize());
		lImg.setHorizontalAlignment(SwingConstants.CENTER);		
		lImg.setBorder(new RoundBorder(isReceived ? getColor(user.getUserId()) : new Color(51, 204, 255)));

		imgLine.add(lImg);
		imgLine.add(Box.createGlue());
				
		JButton emojiButton = null;
		if (isReceived) {
			emojiButton = createEmojiButton(messageId, controller);	
			imgLine.add(emojiButton);
			imgLine.add(Box.createRigidArea(new Dimension(15, 1)));
		}
		
		JPanel secondLine = new JPanel();
		secondLine.setLayout(new FlowLayout(FlowLayout.RIGHT));
		secondLine.setPreferredSize(new Dimension(ChatUI.WIDTH - 10, 50));
		emojiComponent.put(messageId, new EmojiComponents(secondLine, emojiButton));

		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.add(imgLine);
		wrapper.add(secondLine);
		
		messagePanel.add(wrapper, index++);
		messagePanel.revalidate();
	}
	
	private JButton createUploadButton() {
		JButton uploadBtn = new JButton();
		URL imgURL = getClass().getResource("/images/clip.png");
		uploadBtn.setIcon(new ImageIcon(imgURL));
		uploadBtn.setBorder(null);
		uploadBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(IMAGE_FILTER);
				chooser.showOpenDialog(null);
				File file = chooser.getSelectedFile();
				if (file == null) {
					return;
				}

				try {
					BufferedImage img = ImageIO.read(file);
					float FACTOR  = (ChatUI.WIDTH * 0.60f) / img.getWidth();
					FACTOR = FACTOR > 1.0 ? 1.0f : FACTOR;
					int scaleX = (int) (img.getWidth() * FACTOR);
					int scaleY = (int) (img.getHeight() * FACTOR);
					Image image = img.getScaledInstance(scaleX, scaleY, Image.SCALE_SMOOTH);
					BufferedImage buffered = new BufferedImage(scaleX, scaleY, BufferedImage.TRANSLUCENT);
					buffered.getGraphics().drawImage(image, 0, 0 , null);	
					controller.sendImage(buffered);	
					
				} catch (IOException e1) {}
			}
		});
		uploadBtn.addMouseListener(new MouseListener() {
			@Override
			public void mouseExited(MouseEvent e) {
				uploadBtn.setBorder(new LineBorder(UIManager.getColor("control")));
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				uploadBtn.setBorder(new LineBorder(Color.RED));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		return uploadBtn;
	}

	public Component getChatPanel() {
		return chatPanel;
	}
	
	public void addDebugText(String text) {
		debugPanel.addText(text);
	}

	public void setFocus() {
		inputText.grabFocus();
	}
}
