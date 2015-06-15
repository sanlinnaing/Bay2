package com.sanlin.bay2keyboard;

import android.text.SpannableStringBuilder;
import android.util.Log;

public class BamarPrime {
	public static boolean e_vowel = false;
	private static boolean e_cons_swapped = false;
	private static boolean e_cons_medial_swapped = false;
	private static char[] medialStack = new char[3];
	private static int medialCount = 0;

	public BamarPrime() {

	}

	public static SpannableStringBuilder PrimeBook(
			SpannableStringBuilder mComposing, int typedCode, boolean doubleTapped) {
		if (typedCode == 0x1031) {
			e_vowel = true;
			e_cons_swapped = false;
			e_cons_medial_swapped = false;
			medialCount = 0;
			char[] tmp={(char)0x200B,(char)0x1031};
			mComposing.append(String.valueOf(tmp));
			return mComposing;
		}
		if(e_vowel&&(e_cons_swapped||e_cons_medial_swapped)&&doubleTapped){
			String tmp=reOrderEVowel(mComposing.toString(),typedCode);
			mComposing.clear();
			mComposing.append(tmp);
			
			return mComposing;
		}
		if (e_vowel == true) {
			if ((typedCode >= 0x1000) && (typedCode <= 0x1021)
					&& (!e_cons_swapped)) {
				String tmp = reOrderEVowel(mComposing.toString(), typedCode);
				mComposing.clear();
				mComposing.append(tmp);
				e_cons_swapped = true;
				return mComposing;
			}
			if ((typedCode >= 0x103B) && (typedCode <= 0x103E)) {
				if (isValidMedial(typedCode)) {
					medialStack[medialCount] = (char) typedCode;
					medialCount++;
					e_cons_medial_swapped = true;
					String tmp = reOrderEVowel(mComposing.toString(),
							typedCode);
					mComposing.clear();
					mComposing.append(tmp);
					return mComposing;
				}
			}

		}
		mComposing.append((char) typedCode);
		e_vowel = false;
		e_cons_swapped = false;
		e_cons_medial_swapped = false;
		medialCount = 0;

		return mComposing;
	}

	private static String reOrderEVowel(String string, int primaryCode) {
		// TODO Auto-generated method stub
		Log.d("reOrderEVowelCons",
				" string : " + string + " : " + string.length());
		string = (String) string.subSequence(0, string.length() - 1)
				+ (char) primaryCode + (char) 0x1031;
		return string;
	}

	private static boolean isValidMedial(int primaryCode) {

		if (!e_cons_swapped)// if no previous consonant, it is invalid
			return false;
		else if (!e_cons_medial_swapped)// if no previous medial, no need to
										// check it is
			// valid
			return true;
		else if (medialCount > 2)// only 3 times of medial;
			return false;
		else if (medialStack[medialCount - 1] == 4158)// if previous medial is
														// Ha medial, no other
														// medial followed
			return false;
		else if ((medialStack[medialCount - 1] == 4157)
				&& (primaryCode != 4158))
			// if previous medial is Wa medial, only Ha madial will followed, no
			// other medial followed
			return false;
		else if (((medialStack[medialCount - 1] == 4155) && (primaryCode == 4156))
				// if previous medial Ya medial and then Ra medial followed
				|| ((medialStack[medialCount - 1] == 4156) && (primaryCode == 4155))
				// if previous medial is Ra medial and then Ya medial followed
				|| ((medialStack[medialCount - 1] == 4155) && (primaryCode == 4155))
				// if previous medial is Ya medial and then Ya medial followed
				|| ((medialStack[medialCount - 1] == 4156) && (primaryCode == 4156)))
			// if previous medial is Ra medial and then Ra medial followed
			return false;
		// if All condition is passed, medial is valid :D Bravo
		return true;
	}
}
