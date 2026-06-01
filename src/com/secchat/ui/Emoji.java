package com.secchat.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class Emoji {
	public enum EMOJIS {EMO_SMILE, EMO_HEART, EMO_THUMB_UP, EMO_GRIN_STARS, EMO_THUMB_DOWN, EMO_ANGRY,
			EMO_POOP, EMO_SMILE_PLUS};
	private static final ImageIcon[] IMG_EMOJIS = new ImageIcon[EMOJIS.values().length];
	public static ImageIcon NONE_ICON = null;
	public static EMOJIS NONE_EMOJI = EMOJIS.EMO_SMILE_PLUS;
	
	static {
		int pos = 0;
		for (int i = 0; i < IMG_EMOJIS.length; i++) {
			ImageIcon icon = new ImageIcon(ChatView.class.getResource("/images/" 
					+ EMOJIS.values()[i].name().toLowerCase().substring(4) + ".png"));
			icon = new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			IMG_EMOJIS[pos++] = icon;	
		}
		NONE_ICON = IMG_EMOJIS[pos - 1];
	}
	
	public static interface EmojiPickedListener {
		public void onEmojiPicked(long messageId, EMOJIS newEmoji, EMOJIS oldEmoji);
	}

	private static EMOJIS getOldEmoji(JButton emojiButton) {
		for (int i = 0; i < IMG_EMOJIS.length; i++) {
			if (emojiButton.getIcon() == IMG_EMOJIS[i]) {
				return EMOJIS.values()[i];
			}
		}
 		return EMOJIS.EMO_SMILE_PLUS;
	}
	
	public static JPopupMenu createEmojiMenu(final long messageId, final JButton emojiButton,
			final EmojiPickedListener listener) {
		final JPopupMenu popup = new JPopupMenu();
		
		for (int i = 0; i < IMG_EMOJIS.length; i++) {
			JMenuItem menuItem = new JMenuItem(IMG_EMOJIS[i]);
			final ImageIcon icon = IMG_EMOJIS[i];
			final EMOJIS emoji = EMOJIS.values()[i];
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (icon != emojiButton.getIcon()) {
						EMOJIS oldEmoji = getOldEmoji(emojiButton);
						emojiButton.setIcon(icon);
						emojiButton.validate();
						listener.onEmojiPicked(messageId, emoji, oldEmoji);
					}
				}
			});
			
			popup.add(menuItem);
		}
		return popup;
	}

	public static ImageIcon getIcon(String emoji) {
		try {
			EMOJIS emo = EMOJIS.valueOf(emoji);
			if (emo == null) {
				return null;
			}
			return IMG_EMOJIS[emo.ordinal()];
		} catch (IllegalArgumentException | NullPointerException ex) {
			return null;
		}
	}

}
