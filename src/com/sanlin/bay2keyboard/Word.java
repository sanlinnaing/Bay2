package com.sanlin.bay2keyboard;

public class Word {
	private long id;
	private String word;
	private int freq;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return word;
	}
}