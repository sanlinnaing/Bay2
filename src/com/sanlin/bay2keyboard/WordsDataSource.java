package com.sanlin.bay2keyboard;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class WordsDataSource {

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
			MySQLiteHelper.COLUMN_WORD, MySQLiteHelper.COLUMN_FREQ };

	public WordsDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Word createWord(String comment) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_WORD, comment);
		values.put(MySQLiteHelper.COLUMN_FREQ, "1");
		long insertId = database.insert(MySQLiteHelper.TABLE_WORDS, null,
				values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_WORDS, allColumns,
				MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null,
				null);
		cursor.moveToFirst();
		Word newComment = cursorToWord(cursor);
		cursor.close();
		return newComment;
	}

	public void deleteComment(Word comment) {
		long id = comment.getId();
		System.out.println("Comment deleted with id: " + id);
		database.delete(MySQLiteHelper.TABLE_WORDS, MySQLiteHelper.COLUMN_ID
				+ " = " + id, null);
	}

	public List<Word> getAllWords() {
		List<Word> words = new ArrayList<Word>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_WORDS, allColumns,
				null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Word word = cursorToWord(cursor);
			words.add(word);
			cursor.moveToNext();
		}
		// make sure to close the cursor
		cursor.close();
		return words;
	}

	public List<Word> getLikedWords(String queryWord) {
		List<Word> likeWords = new ArrayList<Word>();
		String[] args = new String[1];
		args[0] = queryWord + "%";
		// Cursor friendLike =
		// db.rawQuery("SELECT * FROM songs WHERE SongName like ?", args);
		Cursor cursor = database.rawQuery("SELECT * FROM "
				+ MySQLiteHelper.TABLE_WORDS + " WHERE "+MySQLiteHelper.COLUMN_WORD+" like ? order by freq", args);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Word word = cursorToWord(cursor);
			likeWords.add(word);
			cursor.moveToNext();
		}
		// make sure to close the cursor
		cursor.close();
		return likeWords;
	}
	public List<String> getSuggestedWords(String queryWord) {
		List<String> likeWords = new ArrayList<String>();
		String[] args = new String[1];
		args[0] = queryWord + "%";
		Cursor cursor = database.rawQuery("SELECT * FROM "
				+ MySQLiteHelper.TABLE_WORDS + " WHERE "+MySQLiteHelper.COLUMN_WORD+" like ? order by freq", args);

		cursor.moveToFirst();
		int i=0;
		while (!cursor.isAfterLast()&&i<10) {
			Word word = cursorToWord(cursor);
			likeWords.add(word.toString());
			cursor.moveToNext();
			i++;
		}
		// make sure to close the cursor
		cursor.close();
		return likeWords;
	}

	private Word cursorToWord(Cursor cursor) {
		Word word = new Word();
		word.setId(cursor.getLong(0));
		word.setWord(cursor.getString(1));
		word.setFreq(cursor.getInt(2));
		return word;
	}
}
