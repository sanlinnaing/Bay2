package com.sanlin.bay2keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sanlin.bay2keyboard.R;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class Bay2IME extends InputMethodService implements
		KeyboardView.OnKeyboardActionListener {
	private boolean mCompletionOn;
	private boolean mPredictionOn;
	private CandidateView mCandidateView;
	private CompletionInfo[] mCompletions;
	private List<String> mSuggestions;
	private static long mLastShiftTime = 0;
	private static long mLastKeyTime = 0;
	private static boolean doubleTapped=false;
	private static String mWordSeparators = null;
	private BKeyboardView mInputView;
	private InputMethodManager mInputMethod;
	private BKeyboard mQwertyKeyboard;
	private BKeyboard mBamarKeyboard;
	private BKeyboard mBamarShiftedKeyboard;
	private BKeyboard mCurKeyboard;
	private SpannableStringBuilder mComposing = new SpannableStringBuilder();
	private boolean mCapsLock = false;
	
	private int voiceCount = 0;
	private String[] voice = { null, null, null, null };
	// for Work segmentation
	private final static String cons = "[\u1000-\u1021\u1023-\u1027\u1029\u102A\u103F]";
	// consonants + independent_vowel (!caution! 103F is need extra requirement)
	private final static String medial = "([\u103B-\u103E])*";
	private final static String vowel = "([\u102B-\u1032](\u103A)?)*";
	private final static String cons_final = "(" + cons + "\u103A)?";
	private final static String tone = "([\u1036\u1037\u1038])*";
	private final static String digit = "[\u1040-\u1049]+";
	private final static String symbol = "[\u104C\u104D\u104F]|\u104E\u1004\u103A\u1038|\u104E\u1004\u103A\u1039|\u104E";
	private final static String independent_vowel = "[\u1023-\u1027\u1029\u102A]";
	String wordSegmentPattern = cons + "\u103A(\u103B)?|" + "(" + cons
			+ "[\u1004\u101B]\u103A\u1039)?(" + cons + medial + vowel + cons
			+ "\u1039)?" + cons + medial + vowel + cons_final + tone + "("
			+ cons + "\u103A(\u103B)?)*";
	Pattern wSegmentPtn = Pattern.compile(wordSegmentPattern);

	private WordsDataSource datasource;
	//

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mInputMethod = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mWordSeparators = getResources().getString(R.string.word_separators);
		datasource = new WordsDataSource(this);
	    datasource.open();

	}

	@Override
	public void onInitializeInterface() {
		// TODO Auto-generated method stub
		mQwertyKeyboard = new BKeyboard(this, R.xml.qwerty);
		mBamarKeyboard = new BKeyboard(this, R.xml.my_qwerty);
		mBamarShiftedKeyboard = new BKeyboard(this, R.xml.my_shifted_qwerty);
		mCurKeyboard = mBamarKeyboard;
	}

	@Override
	public View onCreateInputView() {
		mInputView = (BKeyboardView) getLayoutInflater().inflate(
				R.layout.input, null);
		mInputView.setOnKeyboardActionListener(this);
		mInputView.setKeyboard(mBamarKeyboard);

		return mInputView;
	}

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		// TODO Auto-generated method stub
		super.onStartInputView(info, restarting);
		// mCurKeyboard = mQwertyKeyboard;
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		// TODO Auto-generated method stub
		super.onStartInput(attribute, restarting);
		mComposing.clear();

		mPredictionOn = false;
		mCompletionOn = false;
		mCompletions = null;

		switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
		case InputType.TYPE_CLASS_TEXT:
			// This is general text editing. We will default to the
			// normal alphabetic keyboard, and assume that we should
			// be doing predictive text (showing candidates as the
			// user types).
			mCurKeyboard = mBamarKeyboard;
			mPredictionOn = true;

			// We now look for a few special variations of text that will
			// modify our behavior.
			int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
			if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
				// Do not display predictions / what the user is typing
				// when they are entering a password.
				mPredictionOn = false;
			}

			if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
					|| variation == InputType.TYPE_TEXT_VARIATION_URI
					|| variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
				// Our predictions are not useful for e-mail addresses
				// or URIs.
				mPredictionOn = false;
			}

			if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
				// If this is an auto-complete text view, then our predictions
				// will not be shown and instead we will allow the editor
				// to supply their own. We only show the editor's
				// candidates when in fullscreen mode, otherwise relying
				// own it displaying its own UI.
				mPredictionOn = false;
				// mCompletionOn = isFullscreenMode();
			}

			// We also want to look at the current state of the editor
			// to decide whether our alphabetic keyboard should start out
			// shifted.
			updateShiftKeyState(attribute);
			break;

		default:
			// For all unknown input types, default to the alphabetic
			// keyboard with no special features.
			mCurKeyboard = mQwertyKeyboard;
			updateShiftKeyState(attribute);
		}

		mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
		BamarPrime.e_vowel = false;
	}

	@Override
	public void onFinishInput() {
		// TODO Auto-generated method stub
		super.onFinishInput();
		updateCandidates();

		// We only hide the candidates window when finishing input on
		// a particular editor, to avoid popping the underlying application
		// up and down if the user is entering text into the bottom of
		// its window.
		setCandidatesViewShown(false);
		if (mInputView != null) {
			mCurKeyboard = (BKeyboard) mInputView.getKeyboard();
			mInputView.closing();
		}
	}

	@Override
	public View onCreateCandidatesView() {
		Log.d("onCreateCandidatesView","onCreateCandidatesView");
		mCandidateView = new CandidateView(this);
		mCandidateView.setService(this);
		return mCandidateView;
	}

	/**
	 * Update the list of available candidates from the current composing text.
	 * This will need to be filled in by however you are determining candidates.
	 */
	private void updateCandidates() {
		Log.d("updateCandidates", "list update");

		if (mPredictionOn) {
			if (mComposing.length() > 0) {
				List<String> list = new ArrayList<String>();
				list=datasource.getSuggestedWords(mComposing.toString());
				setSuggestions(list, true, true);
			} else {
				setSuggestions(null, false, false);
			}
		}
	}

	public void setSuggestions(List<String> suggestions, boolean completions,
			boolean typedWordValid) {
		if (suggestions != null && suggestions.size() > 0) {
			setCandidatesViewShown(true);
		} else if (isExtractViewShown()) {
			setCandidatesViewShown(true);
		}
		if (mCandidateView != null) {
			mCandidateView.setSuggestions(suggestions, completions,
					typedWordValid);
			mSuggestions=suggestions;
		}
	}

	/**
	 * Deal with the editor reporting movement of its cursor.
	 */
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);
		Log.d("onUpdateSelection","onUpdateSelection");
		// If the current selection in the text view changes, we should
		// clear whatever candidate text we have.
//		if (mComposing.length() > 0
//				&& (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
//			Log.d("onUpdateSelection","conditions = true");
//			mComposing.clear();
//			updateCandidates();
//			InputConnection ic = getCurrentInputConnection();
//			if (ic != null) {
//				ic.finishComposingText();
//			}
//		}
	}

	/**
	 * This tells us about completions that the editor has determined based on
	 * the current text in it. We want to use this in fullscreen mode to show
	 * the completions ourself, since the editor can not be seen in that
	 * situation.
	 */
	@Override
	public void onDisplayCompletions(CompletionInfo[] completions) {
		Log.d("onDisplayCompletions","onDisplayCompletions");
		if (mCompletionOn) {
			mCompletions = completions;
			if (completions == null) {
				Log.d("onDisplayCompletions","completions null");
				setSuggestions(null, false, false);
				return;
			}

			List<String> stringList = new ArrayList<String>();
			for (int i = 0; i < completions.length; i++) {
				CompletionInfo ci = completions[i];
				if (ci != null)
					stringList.add(ci.getText().toString());
			}
			setSuggestions(stringList, true, true);
		}
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		if ((keyCodes != null) && (keyCodes.length >= 2))
			Log.d("onKey", "keyCodes[] = " + keyCodes[0] + " : " + keyCodes[1]+"primaryCode "+primaryCode);

		if (isWordSeparator(primaryCode)) {
			// Handle separator
			if (mComposing.length() > 0) {
				commitTyped(getCurrentInputConnection());
			}
			sendKey(primaryCode);
			updateShiftKeyState(getCurrentInputEditorInfo());
		} else if (primaryCode == Keyboard.KEYCODE_DELETE) {
			handleBackspace();
		} else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
			handleShift();
		} else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
			handleClose();
			return;
		} else {
			handleCharacter(primaryCode, keyCodes);
		}

	}

	private void sendKey(int keyCode) {
		switch (keyCode) {
		case '\n':
			keyDownUp(KeyEvent.KEYCODE_ENTER);
			break;
		default:
			if (keyCode >= '0' && keyCode <= '9') {
				keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
			} else {
				getCurrentInputConnection().commitText(
						String.valueOf((char) keyCode), 1);
			}
			break;
		}
	}

	/**
	 * Helper function to commit any text being composed in to the editor.
	 */
	private void commitTyped(InputConnection inputConnection) {
		Log.d("commitTyped","commit");
		if (mComposing.length() > 0) {
			mComposing.clearSpans();
			inputConnection.commitText(mComposing, mComposing.length());
			mComposing.clear();
			updateCandidates();
			BamarPrime.e_vowel = false;
		}
	}

	private void setSpannable() {
		mComposing.setSpan(new BackgroundColorSpan(0x65ff0000), 0,
				mComposing.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		mComposing.setSpan(new ForegroundColorSpan(0xffffffff), 0,
				mComposing.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

	}

	private void spannableSetComposing() {
		setSpannable();
		getCurrentInputConnection().setComposingText(mComposing, 1);
	}

	// need to change to Bamar Language transition
	// need to change to English language transition

	private void handleCharacter(int primaryCode, int[] keyCodes) {
		/*
		 * if (isInputViewShown()) { if (mInputView.isShifted()) { primaryCode =
		 * Character.toUpperCase(primaryCode); } }
		 */
		doubleTapped=checkDoubleTap(primaryCode, keyCodes);
		if (isBamarAlphabet(primaryCode)) {
			mComposing = BamarPrime.PrimeBook(mComposing, primaryCode,doubleTapped);
			Log.d("handlePrime", "mComposing = " + mComposing.toString());
			mComposing.clearSpans();
			// setSpannable();
			spannableSetComposing();

			if ((mComposing.length() == 2) && (primaryCode == 0x1031)) {
				spannableSetComposing();
			} else if (!isOneBamarVoice(mComposing)) {
				if (voiceCount >= 2) {
					String voices = voice[1];
					for (int i = 2; i < voiceCount; i++)
						voices = voices + voice[i];
					mComposing.clear();
					mComposing.append(voices);
					mComposing.clearSpans();
					// setSpannable();
				}
				if (voiceCount == 0) {
					commitTyped(getCurrentInputConnection());
					// updateShiftKeyState(getCurrentInputEditorInfo());
				} else {
					getCurrentInputConnection().commitText(voice[0], 1);

					spannableSetComposing();
				}
			}
			updateShiftKeyState(getCurrentInputEditorInfo());
			updateCandidates();
		} else {
			if (mComposing.length() > 0) {
				commitTyped(getCurrentInputConnection());
				updateShiftKeyState(getCurrentInputEditorInfo());

			}
			getCurrentInputConnection().commitText(
					String.valueOf((char) primaryCode), 1);
		}

	}

	private boolean checkDoubleTap(int primaryCode, int[] keyCodes) {
		long now = System.currentTimeMillis();
		if((keyCodes!=null)&&(keyCodes.length>=2))
		{
			if((primaryCode==keyCodes[1])){
				if(now-mLastKeyTime<800){
					Log.d("checkDoubleTap","Double Tapped");
					return true;
					//now=now;
				}
			}
			
		}
		mLastKeyTime=now;
		return false;
	}

	private boolean isOneBamarVoice(SpannableStringBuilder mComposing2) {
		Matcher wordMatcher = wSegmentPtn.matcher(mComposing2);
		Log.d("isOneBarmaVoice", " : " + mComposing2);
		int count = 0;
		String found = null;
		while (wordMatcher.find()) {
			found = wordMatcher.group();
			if (found != "") {
				count++;
				voice[count - 1] = found;
			}
		}
		voiceCount = count;
		boolean two_voices_end_with_cons = false;
		if ((count == 2)
				&& ((mComposing2.toString().charAt(mComposing2.length() - 1) >= 0x1000)
						&& (mComposing2.toString().charAt(
								mComposing2.length() - 1) <= 0x1021) || (mComposing2
						.toString().charAt(mComposing2.length() - 1) == 0x1039)))
			two_voices_end_with_cons = true;
		return (two_voices_end_with_cons) || (count == 1);
	}

	/**
	 * Helper to determine if a given character code is alphabetic.
	 */
	private boolean isBamarAlphabet(int code) {
		// if (Character.isLetter(code)) {
		if ((code >= 0x1000) && (code <= 0x103F)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Helper to update the shift state of our keyboard based on the initial
	 * editor state.
	 */
	private void updateShiftKeyState(EditorInfo attr) {
		if (attr != null && mInputView != null
		/* && mQwertyKeyboard == mInputView.getKeyboard() */) {
			int caps = 0;
			EditorInfo ei = getCurrentInputEditorInfo();
			if (ei != null && ei.inputType != InputType.TYPE_NULL) {
				caps = getCurrentInputConnection().getCursorCapsMode(
						attr.inputType);
			}
			mInputView.setShifted(mCapsLock || caps != 0);
			updateKeyboard(mInputView.isShifted());
		}
	}

	private void updateKeyboard(boolean shifted) {
		if (shifted) {
			mCurKeyboard = mBamarShiftedKeyboard;

		} else {
			mCurKeyboard = mBamarKeyboard;
		}

		mInputView.setKeyboard(mCurKeyboard);

	}

	private void handleShift() {
		if (mInputView == null) {
			return;
		}

		// Keyboard currentKeyboard = mInputView.getKeyboard();
		if (mQwertyKeyboard == mCurKeyboard)
			// Alphabet keyboard
			checkToggleCapsLock();
		mInputView.setShifted(mCapsLock || !mInputView.isShifted());
		updateKeyboard(mInputView.isShifted());
		// }
	}

	private void checkToggleCapsLock() {
		long now = System.currentTimeMillis();
		if (mLastShiftTime + 800 > now) {
			mCapsLock = !mCapsLock;
			mLastShiftTime = 0;
		} else {
			mLastShiftTime = now;
		}
	}

	private void handleBackspace() {
		final int length = mComposing.length();
		// Log.d("handleBackspace", String.valueOf(length));
		if (length > 1) {
			mComposing.delete(length - 1, length);
			spannableSetComposing();
			updateCandidates();
		} else if (length > 0) {
			mComposing.clear();
			getCurrentInputConnection().commitText("", 0);
			updateCandidates();
		} else {
			keyDownUp(KeyEvent.KEYCODE_DEL);
		}
		// updateShiftKeyState(getCurrentInputEditorInfo());
	}

	private void handleClose() {
		commitTyped(getCurrentInputConnection());
		requestHideSelf(0);
		mInputView.closing();
	}

	private void keyDownUp(int keyEventCode) {
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}

	private static String getWordSeparators() {
		return mWordSeparators;
	}

	public static boolean isWordSeparator(int code) {
		String separators = getWordSeparators();
		return separators.contains(String.valueOf((char) code));
	}

	public static void deleteHandle(InputConnection ic) {
		CharSequence ch = ic.getTextBeforeCursor(1, 0);
		if (ch.length() > 0) {
			Log.d("Delete", "CharSequence length= " + ch.length() + ": char= "
					+ ch.toString());

			if (Character.isLowSurrogate(ch.charAt(0))
					|| Character.isHighSurrogate(ch.charAt(0))) {
				ic.deleteSurroundingText(2, 0);
			} else
				ic.deleteSurroundingText(1, 0);
		} else
			ic.deleteSurroundingText(1, 0);
	}

	public void pickSuggestionManually(int index) {
		Log.d("suggestManually","Manually Suggest "+index);
		if (mPredictionOn && mSuggestions != null && index >= 0
				&& index < mSuggestions.size()) {
			//CompletionInfo ci = mCompletions[index];
			//getCurrentInputConnection().commitCompletion(ci);
			getCurrentInputConnection().commitText(mSuggestions.get(index), 1);
			mComposing.clear();
			if (mCandidateView != null) {
				mCandidateView.clear();
			}
			updateShiftKeyState(getCurrentInputEditorInfo());
		} else if (mComposing.length() > 0) {
			// If we were generating candidate suggestions for the current
			// text, we would commit one of them here. But for this sample,
			// we will just commit the current text.
			commitTyped(getCurrentInputConnection());
		}
	}

	@Override
	public void onPress(int primaryCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRelease(int primaryCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onText(CharSequence text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeDown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeLeft() {
		// TODO Auto-generated method stub
		handleBackspace();

	}

	@Override
	public void swipeRight() {
		// TODO Auto-generated method stub

	}

	@Override
	public void swipeUp() {
		// TODO Auto-generated method stub

	}

}
