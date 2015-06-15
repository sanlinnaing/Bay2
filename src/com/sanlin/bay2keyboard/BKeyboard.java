package com.sanlin.bay2keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;

public class BKeyboard extends Keyboard {
	private Key mEnterKey;
	private Key mSpaceKey;

	public BKeyboard(Context context, int xmlLayoutResId) {
		super(context, xmlLayoutResId);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
			XmlResourceParser parser) {
		Key key = new BKey(res, parent, x, y, parser);
		if (key.codes[0] == 10) {
			mEnterKey = key;
		} else if (key.codes[0] == ' ') {
			mSpaceKey = key;
		}
		return key;
	}

	void setImeOptions(Resources res, int options) {
		if (mEnterKey == null) {
			return;
		}

		switch (options
				& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
		case EditorInfo.IME_ACTION_GO:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_go_key);
			break;
		case EditorInfo.IME_ACTION_NEXT:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_next_key);
			break;
		case EditorInfo.IME_ACTION_SEARCH:
			mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
			mEnterKey.label = null;
			break;
		case EditorInfo.IME_ACTION_SEND:
			mEnterKey.iconPreview = null;
			mEnterKey.icon = null;
			mEnterKey.label = res.getText(R.string.label_send_key);
			break;
		default:
			mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
			mEnterKey.label = null;
			break;
		}
	}

	void setSpaceIcon(final Drawable icon) {
		if (mSpaceKey != null) {
			mSpaceKey.icon = icon;
		}
	}

	static class BKey extends Keyboard.Key {

		public BKey(Resources res, Keyboard.Row parent, int x, int y,
				XmlResourceParser parser) {
			super(res, parent, x, y, parser);
		}

		/**
		 * Overriding this method so that we can reduce the target area for the
		 * key that closes the keyboard.
		 */
		@Override
		public boolean isInside(int x, int y) {
			return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
		}
	}
}
