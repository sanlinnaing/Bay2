package com.sanlin.bay2keyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;

public class BKeyboardView extends KeyboardView {

	public BKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean onLongPress(Key key) {
		if (key.codes[0] == '\n') {
			// getOnKeyboardActionListener().onKey('\n', null);
			Log.d("LongPress = ", "Enter Key");
			return true;
		}
		if (key.codes[0] == -101) {
			Log.d("LongPress = ", "Language Switch Key");
			return true;

		} else {
			return super.onLongPress(key);
		}
	}
}
